import { useEffect, useState } from 'react';
import { facebookApi } from '../../lib/apiClient';
import { useI18n } from '../../i18n/I18nContext';

const REFRESH_INTERVAL_MS = 10000;

export function FacebookSessionCard({ token }) {
  const { t } = useI18n();
  const [status, setStatus] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    const refresh = () => facebookApi.browserStatus(token)
      .then((value) => { if (active) { setStatus(value); setError(''); } })
      .catch((failure) => { if (active) setError(failure.message); });
    refresh();
    const timer = window.setInterval(refresh, REFRESH_INTERVAL_MS);
    return () => { active = false; window.clearInterval(timer); };
  }, [token]);

  async function run(action) {
    setBusy(true);
    setError('');
    try {
      await action(token);
      setStatus(await facebookApi.browserStatus(token));
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  const browserKey = status ? `admin.browser.${status.browser.toLowerCase()}` : 'admin.browser.loading';
  const authKey = status ? `admin.browser.auth.${status.authentication.toLowerCase()}` : 'admin.browser.loading';

  return <section className="facebook-session-card" aria-labelledby="facebook-session-title">
    <div className="facebook-session-heading"><div><p className="eyebrow">{t('admin.browser.eyebrow')}</p><h3 id="facebook-session-title">{t('admin.browser.title')}</h3><p>{t('admin.browser.description')}</p></div><span className={`browser-state ${status?.browser === 'READY' ? 'ready' : 'unavailable'}`}>{t(browserKey)}</span></div>
    <div className="facebook-session-stats"><div><span>{t('admin.browser.authLabel')}</span><strong>{t(authKey)}</strong></div><div><span>{t('admin.browser.tabsLabel')}</span><strong>{status ? `${status.activeTabs} / ${status.maxConcurrentTabs}` : '—'}</strong></div><div><span>{t('admin.browser.loginTabLabel')}</span><strong>{status ? t(status.loginPageOpen ? 'admin.browser.open' : 'admin.browser.closed') : '—'}</strong></div></div>
    <p className="facebook-session-hint">{t('admin.browser.remoteHint')}</p>
    {(error || status?.message) && <p className="facebook-session-error" role="alert">{error || status.message}</p>}
    <div className="facebook-session-actions"><button type="button" className="ghost-button" disabled={busy} onClick={() => run(facebookApi.openBrowser)}>{t('admin.browser.openTab')}</button><button type="button" className="ghost-button" disabled={busy || status?.activeTabs > 0} onClick={() => run(facebookApi.retryBrowser)}>{t('admin.browser.retry')}</button></div>
  </section>;
}
