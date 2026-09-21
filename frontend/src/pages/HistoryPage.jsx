import { EmptyState } from './LeadsPage';
import { useI18n } from '../i18n/I18nContext';

export function HistoryPage({ items, onClear }) {
  const { t, locale } = useI18n();
  return <section className="page-panel"><div className="panel-head"><div><p className="eyebrow">{t('common.workspaceActivity')}</p><h2>{t('history.title')}</h2><p className="panel-subtitle">{t('history.text')}</p></div><button className="danger-button" disabled={!items.length} onClick={onClear}>{t('history.clear')}</button></div>{!items.length ? <EmptyState icon="◷" title={t('history.empty')} text={t('history.emptyText')} /> : <div className="history-list">{items.map((item) => <article className="history-row" key={item.id}><div className="history-icon">↗</div><div className="history-main"><strong>{item.url}</strong><small>{new Date(item.createdAt).toLocaleString(locale)}</small></div><div className="history-count"><b>{item.count}</b><span>{t('history.comments')}</span></div><span className="history-status">{t('history.done')}</span></article>)}</div>}</section>;
}
