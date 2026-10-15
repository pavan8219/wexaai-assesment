import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as d3 from 'd3';

import {
  AccountInvestigation,
  GraphNode,
  InvestigationGraph
} from '../../models/investigation.model';

import { InvestigationService } from '../../services/investigation.service';

@Component({
  selector: 'app-investigation',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './investigation.component.html',
  styleUrl: './investigation.component.css'
})
export class InvestigationComponent
  implements AfterViewInit, OnDestroy {

  @ViewChild('graphContainer')
  graphContainer!: ElementRef<HTMLDivElement>;

  accountId = 'A001';

  investigation: AccountInvestigation | null = null;

  graphData: InvestigationGraph | null = null;

  loading = false;

  error = '';

  private simulation?: d3.Simulation<any, undefined>;

  // Kept so the "Fit to view" button and the auto-fit-on-settle
  // callback can reuse the same zoom behavior/selection.
  private zoomBehavior?: d3.ZoomBehavior<SVGSVGElement, unknown>;
  private svgSelection?: d3.Selection<SVGSVGElement, unknown, null, undefined>;
  private currentNodes: any[] = [];
  private currentWidth = 0;
  private currentHeight = 0;

  constructor(
    private investigationService: InvestigationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngAfterViewInit(): void {
    // Graph container is created only when investigation is loaded.
  }

  // ============================================================
  // SEARCH / INVESTIGATION
  // ============================================================

  search(): void {

    const accountId = this.accountId.trim();

    if (!accountId) {
      this.error = 'Please enter an account ID.';
      return;
    }

    this.loading = true;
    this.error = '';

    this.investigation = null;
    this.graphData = null;

    // Stop previous graph simulation
    this.simulation?.stop();

    this.investigationService
      .investigateAccount(accountId)
      .subscribe({

        next: response => {

          this.investigation = response;

          this.investigationService
            .getAccountGraph(accountId)
            .subscribe({

              next: graph => {

                this.graphData = graph;

                this.loading = false;

                /*
                 * Angular needs to render:
                 *
                 * @if (investigation)
                 *     #graphContainer
                 *
                 * before D3 can access it.
                 */
                this.cdr.detectChanges();

                requestAnimationFrame(() => {

                  if (!this.graphContainer) {
                    console.error('Graph container is not available');
                    return;
                  }

                  this.drawGraph(graph);
                });
              },

              error: err => {
                console.error('Graph API error:', err);
                this.error = 'Unable to load graph data.';
                this.loading = false;
              }
            });
        },

        error: err => {
          console.error('Investigation API error:', err);
          this.error = 'Unable to investigate this account.';
          this.loading = false;
        }
      });
  }

  // ============================================================
  // NODE RADIUS (shared by circles, link trimming, and clamping)
  // ============================================================

  private getNodeRadius(node: any): number {

    if (node.id === this.accountId) {
      return 24;
    }

    if (node.type === 'Merchant') {
      return 20;
    }

    return 16;
  }

  /**
   * Fill/stroke colors per node type. Applied as direct SVG
   * attributes (not just CSS classes) - see the note above
   * drawGraph() on why D3-appended elements need that.
   */
  private getNodeColors(node: any): { fill: string; stroke: string; strokeWidth: number } {

    if (node.id === this.accountId) {
      return { fill: '#dc2626', stroke: '#991b1b', strokeWidth: 3 };
    }

    if (node.type === 'Merchant') {

      if (node.riskLevel === 'HIGH') {
        return { fill: '#dc2626', stroke: '#7f1d1d', strokeWidth: 3 };
      }

      if (node.riskLevel === 'MEDIUM') {
        return { fill: '#f59e0b', stroke: '#92400e', strokeWidth: 2 };
      }

      return { fill: '#22c55e', stroke: '#166534', strokeWidth: 2 };
    }

    if (node.type === 'Device') {
      return { fill: '#8b5cf6', stroke: '#5b21b6', strokeWidth: 2 };
    }

    if (node.type === 'IPAddress') {
      return { fill: '#06b6d4', stroke: '#155e75', strokeWidth: 2 };
    }

    return { fill: '#3b82f6', stroke: '#1e40af', strokeWidth: 2 };
  }

  // ============================================================
  // DRAW GRAPH
  // ============================================================

  /*
   * NOTE ON STYLING: everything below sets fill/stroke/font
   * attributes directly on each D3-appended element, rather
   * than relying only on the CSS classes in
   * investigation.component.css. Angular's default view
   * encapsulation scopes that stylesheet to elements Angular
   * itself renders from the template; elements D3 appends
   * straight to the DOM never get that scoping attribute, so
   * the scoped CSS silently fails to match them (nodes render
   * as default black, lines as invisible/no stroke). The CSS
   * classes are still applied for anyone who later switches
   * this component to ViewEncapsulation.None or wants a global
   * override, but the inline attributes are what actually
   * guarantee correct rendering.
   */
  drawGraph(data: InvestigationGraph): void {

    if (!this.graphContainer) {
      console.error('graphContainer is not available');
      return;
    }

    const element = this.graphContainer.nativeElement;

    // Clear previous graph
    element.innerHTML = '';

    const width = element.clientWidth || 900;
    const height = element.clientHeight || 600;

    this.currentWidth = width;
    this.currentHeight = height;

    const svg = d3
      .select(element)
      .append('svg')
      .attr('width', width)
      .attr('height', height)
      .attr('viewBox', `0 0 ${width} ${height}`)
      .attr('class', 'graph-svg')
      .style('cursor', 'grab');

    /*
     * =========================================================
     * ARROW MARKER
     * Small, fixed-size marker. Links are trimmed to stop at
     * each node's edge (see tick handler below), so the marker
     * itself no longer needs a large refX hack to "reach" past
     * a big circle - that was misaligned since refX=25 fell
     * outside the marker's own 0-10 viewBox.
     * =========================================================
     */

    svg
      .append('defs')
      .append('marker')
      .attr('id', 'arrow')
      .attr('viewBox', '0 -5 10 10')
      .attr('refX', 9)
      .attr('refY', 0)
      .attr('markerWidth', 6)
      .attr('markerHeight', 6)
      .attr('orient', 'auto')
      .append('path')
      .attr('d', 'M0,-5L10,0L0,5')
      .attr('class', 'graph-link-arrow')
      .attr('fill', '#9ca3af');

    // Everything pannable/zoomable lives inside this group.
    const zoomLayer = svg.append('g').attr('class', 'zoom-layer');

    const nodes = data.nodes.map(node => ({ ...node }));
    const links = data.links.map(link => ({ ...link }));

    this.currentNodes = nodes;

    /*
     * =========================================================
     * FORCE SIMULATION
     * =========================================================
     */

    this.simulation?.stop();

    const simulation = d3
      .forceSimulation(nodes as any)
      .force(
        'link',
        d3.forceLink(links as any).id((d: any) => d.id).distance(140)
      )
      .force('charge', d3.forceManyBody().strength(-500))
      .force('center', d3.forceCenter(width / 2, height / 2))
      .force('collision', d3.forceCollide().radius((d: any) => this.getNodeRadius(d) + 12));

    this.simulation = simulation;

    /*
     * =========================================================
     * LINKS
     * =========================================================
     */

    const link = zoomLayer
      .append('g')
      .attr('class', 'links')
      .selectAll('line')
      .data(links)
      .enter()
      .append('line')
      .attr('class', 'graph-link')
      .attr('marker-end', 'url(#arrow)')
      .attr('stroke', '#9ca3af')
      .attr('stroke-width', 2)
      .attr('opacity', 0.7);

    /*
     * =========================================================
     * LINK LABELS
     * A CSS text-stroke halo (see .link-label) keeps these
     * readable when a line passes underneath them, instead of
     * relying on background rects.
     * =========================================================
     */

    const linkLabel = zoomLayer
      .append('g')
      .attr('class', 'link-labels')
      .selectAll('text')
      .data(links)
      .enter()
      .append('text')
      .attr('class', 'link-label')
      .attr('fill', '#4b5563')
      .attr('font-size', 10)
      .attr('font-weight', 600)
      .attr('stroke', '#f5f7fb')
      .attr('stroke-width', 3)
      .attr('paint-order', 'stroke')
      .attr('text-anchor', 'middle')
      .text((d: any) => {
        if (d.amount !== null && d.amount !== undefined) {
          return `\u20B9${d.amount.toLocaleString('en-IN')}`;
        }
        return d.relationship;
      });

    /*
     * =========================================================
     * NODES
     * =========================================================
     */

    const node = zoomLayer
      .append('g')
      .attr('class', 'nodes')
      .selectAll('g')
      .data(nodes)
      .enter()
      .append('g')
      .attr('class', 'graph-node');

    node.call(
      d3
        .drag<any, any>()
        .on('start', (event, d) => {
          if (!event.active) {
            simulation.alphaTarget(0.3).restart();
          }
          d.fx = d.x;
          d.fy = d.y;
        })
        .on('drag', (event, d) => {
          d.fx = event.x;
          d.fy = event.y;
        })
        .on('end', (event, d) => {
          if (!event.active) {
            simulation.alphaTarget(0);
          }
          d.fx = null;
          d.fy = null;
        })
    );

    node
      .append('circle')
      .attr('r', (d: any) => this.getNodeRadius(d))
      .attr('class', (d: any) => this.getNodeClass(d))
      .attr('fill', (d: any) => this.getNodeColors(d).fill)
      .attr('stroke', (d: any) => this.getNodeColors(d).stroke)
      .attr('stroke-width', (d: any) => this.getNodeColors(d).strokeWidth);

    node
      .append('text')
      .attr('dx', (d: any) => this.getNodeRadius(d) + 6)
      .attr('dy', 5)
      .attr('class', 'node-label')
      .attr('fill', '#111827')
      .attr('font-size', 12)
      .attr('font-weight', 600)
      .text((d: any) => d.label);

    // Native tooltip - cheap, useful context on hover.
    node
      .append('title')
      .text((d: any) => {
        const parts = [d.label, `Type: ${d.type}`];
        if (d.riskLevel) {
          parts.push(`Risk: ${d.riskLevel}`);
        }
        return parts.join('\n');
      });

    /*
     * =========================================================
     * ZOOM / PAN
     * Lets the user see the whole network when it's wider than
     * the fixed-height container, and "Fit to view" re-centers
     * it on demand.
     * =========================================================
     */

    const zoom = d3
      .zoom<SVGSVGElement, unknown>()
      .scaleExtent([0.3, 3])
      .on('zoom', (event) => {
        zoomLayer.attr('transform', event.transform);
      });

    svg.call(zoom);

    this.zoomBehavior = zoom;
    this.svgSelection = svg;

    /*
     * =========================================================
     * SIMULATION TICK
     * =========================================================
     */

    simulation.on('tick', () => {

      // Keep every node inside the visible container. Without
      // this, forceManyBody's repulsion can push nodes past the
      // container edge, where overflow:hidden silently clips
      // them - the main reason parts of the graph "disappear".
      const margin = 30;
      nodes.forEach((d: any) => {
        const r = this.getNodeRadius(d);
        d.x = Math.max(r + margin, Math.min(width - r - margin, d.x));
        d.y = Math.max(r + margin, Math.min(height - r - margin, d.y));
      });

      link
        .attr('x1', (d: any) => this.trimmedPoint(d.source, d.target, true).x)
        .attr('y1', (d: any) => this.trimmedPoint(d.source, d.target, true).y)
        .attr('x2', (d: any) => this.trimmedPoint(d.source, d.target, false).x)
        .attr('y2', (d: any) => this.trimmedPoint(d.source, d.target, false).y);

      linkLabel
        .attr('x', (d: any) => (d.source.x + d.target.x) / 2)
        .attr('y', (d: any) => (d.source.y + d.target.y) / 2);

      node.attr('transform', (d: any) => `translate(${d.x},${d.y})`);
    });

    // Once the layout has settled, frame the whole graph so
    // nothing starts off-screen or awkwardly cropped.
    simulation.on('end', () => this.fitToView());
  }

  /**
   * Point where a link should stop at a node's edge (plus a
   * small gap on the target side, so the arrowhead sits just
   * outside the circle instead of overlapping it).
   */
  private trimmedPoint(source: any, target: any, atSource: boolean) {

    const dx = target.x - source.x;
    const dy = target.y - source.y;
    const dist = Math.sqrt(dx * dx + dy * dy) || 1;

    if (atSource) {
      const r = this.getNodeRadius(source);
      return { x: source.x + (dx / dist) * r, y: source.y + (dy / dist) * r };
    }

    const r = this.getNodeRadius(target) + 4;
    return { x: target.x - (dx / dist) * r, y: target.y - (dy / dist) * r };
  }

  // ============================================================
  // FIT TO VIEW
  // ============================================================

  fitToView(): void {

    if (!this.svgSelection || !this.zoomBehavior || this.currentNodes.length === 0) {
      return;
    }

    const padding = 60;

    const xs = this.currentNodes.map((d: any) => d.x);
    const ys = this.currentNodes.map((d: any) => d.y);

    const minX = Math.min(...xs) - padding;
    const maxX = Math.max(...xs) + padding;
    const minY = Math.min(...ys) - padding;
    const maxY = Math.max(...ys) + padding;

    const graphWidth = maxX - minX || 1;
    const graphHeight = maxY - minY || 1;

    const scale = Math.min(
      3,
      Math.max(0.3, Math.min(this.currentWidth / graphWidth, this.currentHeight / graphHeight))
    );

    const translateX = this.currentWidth / 2 - scale * (minX + graphWidth / 2);
    const translateY = this.currentHeight / 2 - scale * (minY + graphHeight / 2);

    this.svgSelection
      .transition()
      .duration(500)
      .call(
        this.zoomBehavior.transform as any,
        d3.zoomIdentity.translate(translateX, translateY).scale(scale)
      );
  }

  // ============================================================
  // NODE CSS CLASS
  // ============================================================

  getNodeClass(node: GraphNode): string {

    if (node.id === this.accountId) {
      return 'node-selected';
    }

    if (node.type === 'Merchant') {

      if (node.riskLevel === 'HIGH') {
        return 'node-high-risk';
      }

      if (node.riskLevel === 'MEDIUM') {
        return 'node-medium-risk';
      }

      return 'node-merchant';
    }

    if (node.type === 'Device') {
      return 'node-device';
    }

    if (node.type === 'IPAddress') {
      return 'node-ip';
    }

    return 'node-account';
  }

  // ============================================================
  // CLEANUP
  // ============================================================

  ngOnDestroy(): void {
    this.simulation?.stop();
  }
}