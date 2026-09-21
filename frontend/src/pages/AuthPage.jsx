import { useState } from 'react';
import { PreferencesControl } from '../components/ui/PreferencesControl';
import { useI18n } from '../i18n/I18nContext';
import { authApi } from '../lib/apiClient';

export function AuthPage({ onLogin, theme, onThemeChange }) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [notice, setNotice] = useState(null);
  const { t } = useI18n();
  const content = mode === 'login'
    ? { title: t('auth.storyLogin'), subtitle: t('auth.storyLoginText') }
    : { title: t('auth.storyRegister'), subtitle: t('auth.storyRegisterText') };

  function changeMode(nextMode) {
    setMode(nextMode);
    setNotice(null);
  }

  async function submit(event) {
    event.preventDefault();
    const email = form.email.trim().toLowerCase();
    if (!email || !form.password || (mode === 'register' && !form.name.trim())) {
      setNotice({ type: 'error', text: t('auth.required') });
      return;
    }
    try {
      if (mode === 'register') {
        await authApi.register({ name: form.name.trim(), email, password: form.password });
        setNotice({ type: 'success', text: t('auth.sent') });
        setMode('login');
        return;
      }
      onLogin(await authApi.login({ email, password: form.password }));
    } catch (error) {
      setNotice({ type: 'error', text: error.message });
    }
  }

  return <main className="auth-shell">
    <div className="auth-preferences"><PreferencesControl theme={theme} onThemeChange={onThemeChange} /></div>
    <section className="auth-story">
      <a className="brand auth-brand" href="#top"><span className="brand-mark">↗</span><span>lead<span>flow</span></span></a>
      <div className="auth-story-copy">
        <span className="pill">{t('common.facebookIntelligence')}</span>
        <h1>{content.title}</h1><p>{content.subtitle}</p>
        <div className="auth-proof"><div><strong>10x</strong><span>{t('tools.more')}</span></div><div><strong>{t('common.oneWorkspace')}</strong><span>{t('tools.workflowTitle')}</span></div></div>
      </div>
      <p className="auth-footer">© 2026 Leadflow</p>
    </section>
    <section className="auth-card-wrap"><div className="auth-card">
      <div className="auth-tabs"><button className={mode === 'login' ? 'active' : ''} onClick={() => changeMode('login')}>{t('auth.login')}</button><button className={mode === 'register' ? 'active' : ''} onClick={() => changeMode('register')}>{t('auth.register')}</button></div>
      <h2>{mode === 'login' ? t('auth.welcome') : t('auth.create')}</h2><p className="auth-muted">{mode === 'login' ? t('auth.access') : t('auth.minute')}</p>
      <form onSubmit={submit}>
        {mode === 'register' && <label>{t('auth.name')}<input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Alex Morgan" /></label>}
        <label>{t('auth.email')}<input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="you@company.com" /></label>
        <label>{t('auth.password')}<input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder="••••••••" /></label>
        <button className="primary-button auth-submit" type="submit">{mode === 'login' ? t('auth.enter') : t('auth.submit')} <span>→</span></button>
      </form>
      {notice && <div className={`notice ${notice.type}`}>{notice.text}</div>}
      <p className="auth-note">{t('auth.note')}</p>
    </div></section>
  </main>;
}
