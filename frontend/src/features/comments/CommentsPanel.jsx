import { useMemo, useState } from 'react';
import { downloadCommentsCsv } from '../../lib/csvExport';
import { isPotentialLead, summarizeComments } from '../../lib/leadScoring';
import { CommentItem } from './CommentItem';
import { useI18n } from '../../i18n/I18nContext';
import { StatCard } from './StatCard';

export function CommentsPanel({ comments, savedIds, onToggleSave, eyebrowKey = 'comments.latest', titleKey = 'comments.thread' }) {
  const [query, setQuery] = useState('');
  const [onlyLeads, setOnlyLeads] = useState(false);
  const { t } = useI18n();
  const summary = summarizeComments(comments);
  const visibleComments = useMemo(() => comments.filter((comment) => {
    const content = `${comment.author} ${comment.message ?? ''}`.toLocaleLowerCase();
    return (!query || content.includes(query.toLocaleLowerCase())) && (!onlyLeads || isPotentialLead(comment));
  }), [comments, onlyLeads, query]);

  return <section className="results-section"><div className="section-title"><div><p className="eyebrow">{t(eyebrowKey)}</p><h2>{t(titleKey)}</h2></div>{comments.length > 0 && <button className="export-button" onClick={() => downloadCommentsCsv(visibleComments, t)}>⇩ {t('common.exportCsv')}</button>}</div>
    {comments.length === 0 ? <div className="empty"><div>⌁</div><h3>{t('comments.empty')}</h3><p>{t('comments.emptyText')}</p></div> : <><div className="stats"><StatCard label={t('comments.total')} value={summary.total} accent="blue" /><StatCard label={t('comments.root')} value={summary.topLevel} accent="violet" /><StatCard label={t('comments.replies')} value={summary.replies} accent="amber" /><StatCard label={t('comments.signals')} value={summary.prospects} accent="green" /></div><div className="toolbar"><div className="search">⌕<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t('comments.search')} /></div><button className={onlyLeads ? 'filter active-filter' : 'filter'} onClick={() => setOnlyLeads((value) => !value)}>◉ {t('comments.onlyLeads')}</button><button className="filter" onClick={() => { setQuery(''); setOnlyLeads(false); }}>{t('comments.reset')}</button><span>{visibleComments.length} {t('common.results')}</span></div><div className="comment-list">{visibleComments.map((comment) => <CommentItem key={comment.id} comment={comment} isSaved={savedIds.has(comment.id)} onToggleSave={() => onToggleSave(comment)} />)}</div></>}
  </section>;
}
