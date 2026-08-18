export interface SuspiciousNetwork {
  sourceAccount: string;
  network: string[];
  merchant: string;
  riskLevel: string;
  hops: number;
}

export interface SharedSignal {
  account1: string;
  account2: string;
  signalType: string;
  sharedSignal: string;
}

export interface AccountInvestigation {
  accountId: string;
  riskScore: number;
  riskLevel: string;
  highRiskMerchants: string[];
  connectedAccounts: string[];
  sharedDevices: string[];
  sharedIps: string[];
  suspiciousNetworks: string[][];
  reasons: string[];
}

export interface GraphNode {
  id: string;
  label: string;
  type: string;
  riskLevel: string | null;
}

export interface GraphLink {
  source: string;
  target: string;
  relationship: string;
  amount: number | null;
}

export interface InvestigationGraph {
  nodes: GraphNode[];
  links: GraphLink[];
}