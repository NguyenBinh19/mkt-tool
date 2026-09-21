import { CommentItem } from '../features/comments/CommentItem';
import { isPotentialLead } from '../lib/leadScoring';
import { useI18n } from '../i18n/I18nContext';
import { Icon } from '../components/ui/Icon';

export function LeadSignalsPage({ comments, savedIds, onToggleSave, onExtract, canExtract }) {
  const leads = comments.filter(isPotentialLead);
  const { t } = useI18n();
  return <section className="page-panel">
    <div className="panel-head"><div><p className="eyebrow">{t('common.leadDiscovery')}</p><h2>{t('signals.title')}</h2><p className="panel-subtitle">{t('signals.text')}</p></div><span className="status-badge available">{t('signals.count', { count: leads.length })}</span></div>
    {leads.length ? <div className="comment-list">{leads.map((comment) => <CommentItem key={comment.id} comment={comment} isSaved={savedIds.has(comment.id)} onToggleSave={() => onToggleSave(comment)} />)}</div>
      : <div className="approval-empty"><span><Icon name="target" size={30} /></span><h3>{comments.length ? t('signals.none') : t('signals.start')}</h3><p>{comments.length ? t('signals.noneText') : t('signals.startText')}</p>{canExtract && <button className="primary-button" onClick={onExtract}>{t('signals.goExtract')} <Icon name="arrow" size={16} /></button>}</div>}
  </section>;
}
