import { useMemo, useState } from 'react';
import { CommentItem } from '../features/comments/CommentItem';
import { downloadLeadsCsv } from '../lib/csvExport';
import { useI18n } from '../i18n/I18nContext';

export function LeadsPage({ leads, onRemove, onClear }) {
  const [query, setQuery] = useState('');
  const { t } = useI18n();
  const filtered = useMemo(
    () => leads.filter((lead) => `${lead.author} ${lead.message ?? ''}`.toLowerCase().includes(query.toLowerCase())),
    [leads, query]
  );

  return <section className="page-panel">
    <div className="panel-head"><div><p className="eyebrow">{t('common.leadWorkspace')}</p><h2>{t('leads.title')}</h2><p className="panel-subtitle">{t('leads.text')}</p></div><div className="panel-actions"><button className="export-button" disabled={!filtered.length} onClick={() => downloadLeadsCsv(filtered, t)}>⇩ {t('common.exportCsv')}</button><button className="danger-button" disabled={!leads.length} onClick={onClear}>{t('common.clearAll')}</button></div></div>
    {!leads.length
      ? <EmptyState icon="☆" title={t('leads.empty')} text={t('leads.emptyText')} />
      : <><div className="lead-toolbar"><div className="search">⌕<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t('leads.search')} /></div><span>{filtered.length} / {leads.length} {t('common.lead')}</span></div><div className="comment-list">{filtered.map((lead) => <CommentItem key={lead.id} comment={lead} isSaved onToggleSave={() => onRemove(lead.id)} />)}</div></>}
  </section>;
}

export function EmptyState({ icon, title, text }) {
  return <div className="empty"><div>{icon}</div><h3>{title}</h3><p>{text}</p></div>;
}
