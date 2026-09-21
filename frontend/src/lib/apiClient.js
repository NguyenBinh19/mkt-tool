const FACEBOOK_API = '/api/facebook';

async function request(path, options) {
  const response = await fetch(`${FACEBOOK_API}${path}`, options);
  const isJson = response.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    throw new Error(typeof body === 'string' ? body : body.message || `Máy chủ trả về lỗi ${response.status}.`);
  }
  return body;
}

export const facebookApi = {
  openBrowser: () => request('/browser'),
  extractComments: (payload) => request('/extract', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
};
