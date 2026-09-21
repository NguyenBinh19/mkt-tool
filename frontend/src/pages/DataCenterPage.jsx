import { downloadCommentsCsv, downloadLeadsCsv } from '../lib/csvExport';
import { Icon } from '../components/ui/Icon';
import { useI18n } from '../i18n/I18nContext';

export function DataCenterPage({ workspace, comments, leads, history, monitorResults, monitorHistory, onClearComments, onClearLeads, onClearHistory, onClearMonitorResults, onNavigate }) {
  const hasData = comments.length || leads.length || history.length || monitorResults.length || monitorHistory.length;
  const { t } = useI18n();

  function confirmClear(label, action) {
    if (window.confirm(t('data.confirm', { label, name: workspace.name }))) action();
  }

  return <section className="data-center-page"><div className="data-center-hero"><div><p className="eyebrow">{t('common.workspaceData')}</p><h2>{t('data.title', { name: workspace.name })}</h2><p>{t('data.description')}</p></div><div className="data-export-actions">{comments.length > 0 && <button className="primary-button" onClick={() => downloadCommentsCsv(comments, t)}>⇩ {t('data.exportComments')}</button>}{leads.length > 0 && <button className="ghost-button" onClick={() => downloadLeadsCsv(leads, t)}>⇩ {t('data.exportLeads')}</button>}</div></div>
    <div className="data-kpis"><Metric icon="chat" label={t('data.latestComments')} value={comments.length} /><Metric icon="users" label={t('data.savedLeads')} value={leads.length} /><Metric icon="search" label={t('data.posts')} value={monitorResults.length} /><Metric icon="clock" label={t('data.extractions')} value={history.length} /></div>
    {!hasData ? <div className="approval-empty"><span><Icon name="chart" size={29} /></span><h3>{t('data.empty')}</h3><p>{t('data.emptyText')}</p></div> : <div className="data-collections">
      <DataCollection title={t('data.commentsTitle')} description={t('data.commentsText')} count={comments.length} icon="chat" page="Extract comment" onNavigate={onNavigate} actionLabel={t('data.clearResult')} onAction={() => confirmClear(t('data.commentsLabel'), onClearComments)} />
      <DataCollection title={t('data.leadsTitle')} description={t('data.leadsText')} count={leads.length} icon="users" page="Khách hàng tiềm năng" onNavigate={onNavigate} actionLabel={t('data.clearLead')} onAction={() => confirmClear(t('data.leadsLabel'), onClearLeads)} />
      <DataCollection title={t('data.monitorTitle')} description={t('data.monitorText', { count: monitorHistory.length })} count={monitorResults.length} icon="search" page="Content monitor" onNavigate={onNavigate} actionLabel={t('data.clearSearch')} onAction={() => confirmClear(t('data.monitorLabel'), onClearMonitorResults)} />
      <DataCollection title={t('data.historyTitle')} description={t('data.historyText')} count={history.length} icon="clock" page="Lịch sử" onNavigate={onNavigate} actionLabel={t('data.clearHistory')} onAction={() => confirmClear(t('data.historyLabel'), onClearHistory)} />
    </div>}
  </section>;
}

function Metric({ icon, label, value }) {
  return <article><Icon name={icon} size={18} /><span>{label}</span><strong>{value}</strong></article>;
}

function DataCollection({ title, description, count, icon, page, onNavigate, actionLabel, onAction }) {
  const { t } = useI18n();
  return <article className="data-collection"><div className="data-collection-icon"><Icon name={icon} size={19} /></div><div><h3>{title}</h3><p>{description}</p></div><div className="data-collection-actions"><strong>{count}</strong><span>{t('common.items')}</span><button type="button" className="data-open" onClick={() => onNavigate(page)}>{t('data.viewAll')}</button>{count > 0 && <button type="button" onClick={onAction}>{actionLabel}</button>}</div></article>;
}
