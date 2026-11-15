import { useEffect, useState } from 'react';
import { CommentsPanel } from '../features/comments/CommentsPanel';
import { workspaceApi } from '../lib/apiClient';
import { EmptyState } from './LeadsPage';
import { useI18n } from '../i18n/I18nContext';

export function HistoryPage({ items, selectedId, workspaceId, token, savedIds, onToggleSave, onOpen, onBack, onClear }) {
  const { t, locale } = useI18n();
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!selectedId) { setDetail(null); return undefined; }
    let active = true;
    setDetail(null);
    setError('');
    setLoading(true);
    workspaceApi.historyDetail(workspaceId, selectedId, token)
      .then((value) => { if (active) setDetail(value); })
      .catch((failure) => { if (active) setError(failure.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [selectedId, workspaceId, token]);

  if (selectedId) {
    const comments = detail?.comments?.map((comment) => ({ ...comment, postUrl: detail.postUrl })) ?? [];
    return <section className="page-panel history-detail">
      <button type="button" className="history-back" onClick={onBack}>← {t('history.back')}</button>
      <div className="panel-head"><div><p className="eyebrow">{t('history.detailEyebrow')}</p><h2>{t('history.detailTitle')}</h2><p className="panel-subtitle">{detail ? new Date(detail.createdAt).toLocaleString(locale) : t('history.loading')}</p></div>{detail && <span className="status-badge available">{detail.commentCount} {t('history.comments')}</span>}</div>
      {detail && <a className="history-post-link" href={detail.postUrl} target="_blank" rel="noreferrer">{detail.postUrl} ↗</a>}
      {loading && <p className="panel-subtitle">{t('history.loading')}</p>}
      {error && <p className="history-error" role="alert">{error}</p>}
      {detail?.legacyResult && <p className="history-legacy">{t('history.legacyNote')}</p>}
      {detail && (comments.length ? <CommentsPanel comments={comments} savedIds={savedIds} onToggleSave={onToggleSave} eyebrowKey="history.detailEyebrow" titleKey="history.commentsTitle" /> : <EmptyState icon="⌁" title={t('history.noComments')} text={t('history.noCommentsText')} />)}
    </section>;
  }

  return <section className="page-panel"><div className="panel-head"><div><p className="eyebrow">{t('common.workspaceActivity')}</p><h2>{t('history.title')}</h2><p className="panel-subtitle">{t('history.text')}</p></div><button className="danger-button" disabled={!items.length} onClick={onClear}>{t('history.clear')}</button></div>{!items.length ? <EmptyState icon="◷" title={t('history.empty')} text={t('history.emptyText')} /> : <div className="history-list">{items.map((item) => <button type="button" className="history-row history-row-button" key={item.id} onClick={() => onOpen(item.id)}><span className="history-icon">↗</span><span className="history-main"><strong>{item.url}</strong><small>{new Date(item.createdAt).toLocaleString(locale)}</small></span><span className="history-count"><b>{item.count}</b><span>{t('history.comments')}</span></span><span className="history-status">{t('history.open')}</span></button>)}</div>}</section>;
}
