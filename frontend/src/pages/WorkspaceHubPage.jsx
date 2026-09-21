import { useMemo, useState } from 'react';
import { Icon } from '../components/ui/Icon';
import { useI18n } from '../i18n/I18nContext';
import { PreferencesControl } from '../components/ui/PreferencesControl';

const EMPTY_FORM = { name: '', industry: '', description: '' };

export function WorkspaceHubPage({ user, workspaces, onCreate, onUpdate, onDelete, onOpen, onLogout, theme, onThemeChange }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [notice, setNotice] = useState(null);
  const { t } = useI18n();
  const editing = useMemo(() => workspaces.find((workspace) => workspace.id === editingId), [editingId, workspaces]);

  function beginCreate() {
    setEditingId('new');
    setForm(EMPTY_FORM);
    setNotice(null);
  }

  function beginEdit(workspace) {
    setEditingId(workspace.id);
    setForm({ name: workspace.name, industry: workspace.industry, description: workspace.description ?? '' });
    setNotice(null);
  }

  function cancel() {
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  function submit(event) {
    event.preventDefault();
    if (!form.name.trim() || !form.industry.trim()) {
      setNotice(t('workspace.required'));
      return;
    }
    const details = { name: form.name.trim(), industry: form.industry.trim(), description: form.description.trim() };
    if (editingId === 'new') onCreate(details);
    else onUpdate(editingId, details);
    cancel();
  }

  function remove(workspace) {
    if (workspaces.length === 1) {
      setNotice(t('workspace.keepOne'));
      return;
    }
    if (window.confirm(t('workspace.deleteConfirm', { name: workspace.name }))) onDelete(workspace.id);
  }

  return <main className="workspace-hub">
    <header className="hub-header"><button className="brand hub-brand" type="button" onClick={onLogout}><span className="brand-mark">↗</span><span>lead<span>flow</span></span></button><div className="hub-account"><PreferencesControl compact theme={theme} onThemeChange={onThemeChange} /><div className="avatar">{user.name.slice(0, 1).toUpperCase()}</div><span>{user.name}</span><button type="button" onClick={onLogout}>{t('common.logout')}</button></div></header>
    <section className="hub-hero"><div><p className="eyebrow">{t('workspace.directory')}</p><h1>{t('workspace.hero1')}<br /><em>{t('workspace.hero2')}</em></h1><p>{t('workspace.heroText')}</p></div><button type="button" className="primary-button hub-create" onClick={beginCreate}>＋ {t('workspace.create')}</button></section>
    {notice && <div className="notice error">! {notice}</div>}
    <section className="hub-section"><div className="hub-section-heading"><div><p className="eyebrow">{workspaces.length} WORKSPACE</p><h2>{t('workspace.choose')}</h2></div><span>{t('workspace.separate')}</span></div><div className="workspace-directory">{workspaces.map((workspace) => <article className="workspace-directory-card" key={workspace.id}><div className="workspace-directory-icon">{workspace.name.slice(0, 1).toUpperCase()}</div><p className="workspace-industry">{workspace.industry}</p><h3>{workspace.name}</h3><p className="workspace-description">{workspace.description || t('workspace.noDescription')}</p><div className="workspace-card-actions"><button type="button" className="ghost-button" onClick={() => beginEdit(workspace)}>{t('common.edit')}</button><button type="button" className="workspace-delete" onClick={() => remove(workspace)}>{t('common.delete')}</button><button type="button" className="primary-button" onClick={() => onOpen(workspace.id)}>{t('workspace.open')} <Icon name="arrow" size={15} /></button></div></article>)}</div></section>
    {editingId && <div className="workspace-modal-backdrop" role="presentation"><section className="workspace-modal" role="dialog" aria-modal="true" aria-labelledby="workspace-form-title"><button className="modal-close" type="button" onClick={cancel} aria-label={t('common.close')}><Icon name="close" /></button><p className="eyebrow">{editingId === 'new' ? t('workspace.new') : t('workspace.edit')}</p><h2 id="workspace-form-title">{editingId === 'new' ? t('workspace.newTitle') : t('workspace.editTitle', { name: editing?.name ?? 'workspace' })}</h2><p>{t('workspace.formText')}</p><form onSubmit={submit}><label>{t('workspace.name')}<input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder={t('workspace.namePlaceholder')} autoFocus /></label><label>{t('workspace.industry')}<input value={form.industry} onChange={(event) => setForm({ ...form, industry: event.target.value })} placeholder={t('workspace.industryPlaceholder')} /></label><label>{t('workspace.note')}<textarea value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} placeholder={t('workspace.notePlaceholder')} rows="3" /></label><div className="modal-actions"><button type="button" className="ghost-button" onClick={cancel}>{t('common.cancel')}</button><button type="submit" className="primary-button">{editingId === 'new' ? t('workspace.create') : t('common.save')} <Icon name="arrow" size={15} /></button></div></form></section></div>}
  </main>;
}
