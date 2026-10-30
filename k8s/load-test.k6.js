import http from 'k6/http';
import { check, fail } from 'k6';

// Script de carga de leitura autenticada para demonstrar o escalonamento horizontal (HPA) da API.
// Projetado exclusivamente para endpoints de leitura (GET), evitando acionar o processador de outbox
// concorrentemente entre múltiplas réplicas (ver k8s/README.md#hpa-e-bloqueio-de-concorrência-da-outbox).
//
// Variáveis de ambiente aceitas:
// - API_URL: endereço base da API (padrão: http://localhost:30082 para o overlay load-test)
// - KEYCLOAK_URL: endereço base do Keycloak (padrão: http://localhost:30081 para o overlay load-test)
// - REALM: realm do Keycloak (padrão: jacafi)
// - CLIENT_ID: client público OIDC (padrão: jacafi-web)
// - USERNAME: usuário de teste (padrão: dev-employee)
// - PASSWORD: senha de teste (padrão: Dev-employee-2026)

const API_URL = __ENV.API_URL || 'http://localhost:30082';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:30081';
const REALM = __ENV.REALM || 'jacafi';
const CLIENT_ID = __ENV.CLIENT_ID || 'jacafi-web';
const USERNAME = __ENV.USERNAME || 'dev-employee';
const PASSWORD = __ENV.PASSWORD || 'Dev-employee-2026';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m30s', target: 80 },
    { duration: '1m30s', target: 80 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export function setup() {
  const tokenUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;
  const payload = {
    client_id: CLIENT_ID,
    grant_type: 'password',
    username: USERNAME,
    password: PASSWORD,
  };

  const res = http.post(tokenUrl, payload);
  if (res.status !== 200) {
    fail(`Falha ao obter token no Keycloak (${res.status}): ${res.body}`);
  }

  return { token: res.json('access_token') };
}

export default function (data) {
  const params = {
    headers: {
      Authorization: `Bearer ${data.token}`,
    },
  };

  const customersRes = http.get(`${API_URL}/api/v1/customers?page=0&size=20`, params);
  check(customersRes, {
    'GET /api/v1/customers respondeu 200': (r) => r.status === 200,
  });

  const queueRes = http.get(`${API_URL}/api/v1/service-orders?page=0&size=20`, params);
  check(queueRes, {
    'GET /api/v1/service-orders respondeu 200': (r) => r.status === 200,
  });
}

