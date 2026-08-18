import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  AccountInvestigation,
  InvestigationGraph
} from '../models/investigation.model';

@Injectable({
  providedIn: 'root'
})
export class InvestigationService {

  private readonly baseUrl =
    'http://localhost:8080/api/investigations';

  constructor(private http: HttpClient) {}

  investigateAccount(accountId: string) {
    return this.http.get<AccountInvestigation>(
      `${this.baseUrl}/accounts/${accountId}`
    );
  }

  getAccountGraph(accountId: string) {
    return this.http.get<InvestigationGraph>(
      `${this.baseUrl}/accounts/${accountId}/graph`
    );
  }
}