import { translations } from '../i18n/translations';

const API_ROOT = '/api';

function translate(key, variables = {}) {
  const language = document.documentElement.lang === 'en' ? 'en' : 'vi';
  const template = translations[language]?.[key] ?? translations.vi[key] ?? key;
  return Object.entries(variables).reduce((message, [name, value]) => message.replaceAll(`{${name}}`, String(value)), template);
}

async function request(path, { token, ...options } = {}) {
  const response = await fetch(`${API_ROOT}${path}`, {
    ...options,
    headers: { ...(options.headers ?? {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) }
  });
  if (response.status === 204) return null;
  const isJson = response.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const codeKey = typeof body === 'object' && body?.code ? `error.${body.code}` : null;
    const localized = codeKey && translations[document.documentElement.lang === 'en' ? 'en' : 'vi']?.[codeKey];
    throw new Error(localized ?? (typeof body === 'string' && body.trim() ? body : translate('api.serverError', { status: response.status })));
  }
  return body;
}

function json(method, payload, token) {
  return { method, token, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) };
}

export const authApi = {
  login: (payload) => request('/auth/login', json('POST', payload)),
  register: (payload) => request('/auth/register', json('POST', payload)),
  me: (token) => request('/auth/me', { token })
};

export const workspaceApi = {
  list: (token) => request('/workspaces', { token }),
  create: (payload, token) => request('/workspaces', json('POST', payload, token)),
  update: (id, payload, token) => request(`/workspaces/${id}`, json('PUT', payload, token)),
  remove: (id, token) => request(`/workspaces/${id}`, { method: 'DELETE', token }),
  leads: (id, token) => request(`/workspaces/${id}/leads`, { token }),
  saveLead: (id, payload, token) => request(`/workspaces/${id}/leads`, json('POST', payload, token)),
  removeLead: (workspaceId, leadId, token) => request(`/workspaces/${workspaceId}/leads/${leadId}`, { method: 'DELETE', token }),
  clearLeads: (id, token) => request(`/workspaces/${id}/leads`, { method: 'DELETE', token }),
  history: (id, token) => request(`/workspaces/${id}/history`, { token }),
  addHistory: (id, payload, token) => request(`/workspaces/${id}/history`, json('POST', payload, token)),
  clearHistory: (id, token) => request(`/workspaces/${id}/history`, { method: 'DELETE', token }),
  monitor: (id, token) => request(`/workspaces/${id}/monitor`, { token }),
  saveMonitor: (id, payload, token) => request(`/workspaces/${id}/monitor/search`, json('POST', payload, token)),
  clearMonitor: (id, token) => request(`/workspaces/${id}/monitor`, { method: 'DELETE', token }),
  clearMonitorResults: (id, token) => request(`/workspaces/${id}/monitor/results`, { method: 'DELETE', token }),
  toggleMonitorStar: (workspaceId, postId, token) => request(`/workspaces/${workspaceId}/monitor/posts/${postId}/star`, { method: 'PATCH', token })
};

export const facebookApi = {
  openBrowser: () => request('/facebook/browser'),
  searchPosts: (payload) => request('/facebook/search', json('POST', payload)),
  extractComments: (payload) => request('/facebook/extract', json('POST', payload))
};
