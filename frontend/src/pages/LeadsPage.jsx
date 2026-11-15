import { useMemo, useState } from 'react';
import { CommentItem } from '../features/comments/CommentItem';
import { downloadLeadsCsv } from '../lib/csvExport';
import { useI18n } from '../i18n/I18nContext';

export function LeadsPage({ leads, suggestions = [], onSave, onRemove, onClear }) {
  const [query, setQuery] = useState('');
  const [view, setView] = useState('all');
  const { t } = useI18n();
  const savedIds = useMemo(() => new Set(leads.map((lead) => lead.id)), [leads]);
  const automatic = useMemo(() => suggestions
    .filter((item) => !savedIds.has(item.comment.id))
    .map((item) => ({ ...item.comment, postUrl: item.postUrl, historyId: item.historyId })), [suggestions, savedIds]);
  const matches = (comment) => `${comment.author ?? ''} ${comment.message ?? ''}`.toLocaleLowerCase().includes(query.toLocaleLowerCase());
  const visibleSaved = leads.filter(matches);
  const visibleAutomatic = automatic.filter(matches);
  const total = leads.length + automatic.length;

  return <section className="page-panel">
    <div className="panel-head"><div><p className="eyebrow">{t('common.leadWorkspace')}</p><h2>{t('leads.title')}</h2><p className="panel-subtitle">{t('leads.text')}</p></div><div className="panel-actions"><button className="export-button" disabled={!visibleSaved.length} onClick={() => downloadLeadsCsv(visibleSaved, t)}>⇩ {t('common.exportCsv')}</button><button className="danger-button" disabled={!leads.length} onClick={onClear}>{t('leads.clearSaved')}</button></div></div>
    {!total
      ? <EmptyState icon="☆" title={t('leads.empty')} text={t('leads.emptyText')} />
      : <><div className="lead-toolbar"><div className="search">⌕<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t('leads.search')} /></div><span>{visibleSaved.length + visibleAutomatic.length} / {total} {t('common.items')}</span></div><div className="noteworthy-filters">{[['all', t('leads.all')], ['saved', t('leads.saved')], ['suggested', t('leads.suggested')]].map(([id, label]) => <button type="button" key={id} className={view === id ? 'filter active-filter' : 'filter'} onClick={() => setView(id)}>{label}</button>)}</div>
        {(view === 'all' || view === 'saved') && visibleSaved.length > 0 && <div className="noteworthy-group"><h3>{t('leads.saved')} <span>{visibleSaved.length}</span></h3><div className="comment-list">{visibleSaved.map((lead) => <CommentItem key={lead.id} comment={lead} isSaved onToggleSave={() => onRemove(lead.id)} />)}</div></div>}
        {(view === 'all' || view === 'suggested') && visibleAutomatic.length > 0 && <div className="noteworthy-group"><h3>{t('leads.suggested')} <span>{visibleAutomatic.length}</span></h3><p>{t('leads.suggestedHelp')}</p><div className="comment-list">{visibleAutomatic.map((comment) => <CommentItem key={comment.id} comment={comment} isSaved={false} onToggleSave={() => onSave(comment)} />)}</div></div>}
        {((view === 'saved' && !visibleSaved.length) || (view === 'suggested' && !visibleAutomatic.length)) && <EmptyState icon="⌕" title={t('leads.noMatch')} text={t('leads.noMatchText')} />}
      </>}
  </section>;
}

export function EmptyState({ icon, title, text }) {
  return <div className="empty"><div>{icon}</div><h3>{title}</h3><p>{text}</p></div>;
}
