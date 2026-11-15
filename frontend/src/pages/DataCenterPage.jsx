import { downloadLeadsCsv } from '../lib/csvExport';
import { Icon } from '../components/ui/Icon';
import { useI18n } from '../i18n/I18nContext';

export function DataCenterPage({ workspace, leads, suggestionCount, history, jobs, activeJobCount = 0, monitorResults, monitorHistory, onClearLeads, onClearHistory, onClearMonitorResults, onNavigate }) {
  const hasData = leads.length || suggestionCount || history.length || jobs?.length || monitorResults.length || monitorHistory.length;
  const { t } = useI18n();

  function confirmClear(label, action) {
    if (window.confirm(t('data.confirm', { label, name: workspace.name }))) action();
  }

  return <section className="data-center-page"><div className="data-center-hero"><div><p className="eyebrow">{t('common.workspaceData')}</p><h2>{t('data.title', { name: workspace.name })}</h2><p>{t('data.description')}</p></div><div className="data-export-actions">{leads.length > 0 && <button className="ghost-button" onClick={() => downloadLeadsCsv(leads, t)}>⇩ {t('data.exportLeads')}</button>}</div></div>
    <div className="data-kpis"><Metric icon="clock" label={t('data.extractions')} value={history.length} /><Metric icon="users" label={t('data.noteworthy')} value={leads.length + suggestionCount} /><Metric icon="search" label={t('data.posts')} value={monitorResults.length} /></div>
    {!hasData ? <div className="approval-empty"><span><Icon name="chart" size={29} /></span><h3>{t('data.empty')}</h3><p>{t('data.emptyText')}</p></div> : <div className="data-collections">
      <DataCollection title={t('data.historyTitle')} description={t('data.historyText')} count={history.length} icon="clock" page="Lịch sử" onNavigate={onNavigate} actionLabel={t('data.clearHistory')} onAction={() => confirmClear(t('data.historyLabel'), onClearHistory)} />
      {jobs && <DataCollection title={t('jobs.title')} description={t('jobs.description')} count={activeJobCount} icon="clock" page="Tác vụ" onNavigate={onNavigate} actionCount={0} />}
      <DataCollection title={t('data.leadsTitle')} description={t('data.leadsText')} count={leads.length + suggestionCount} actionCount={leads.length} icon="users" page="Khách hàng tiềm năng" onNavigate={onNavigate} actionLabel={t('data.clearLead')} onAction={() => confirmClear(t('data.leadsLabel'), onClearLeads)} />
      <DataCollection title={t('data.monitorTitle')} description={t('data.monitorText', { count: monitorHistory.length })} count={monitorResults.length} icon="search" page="Content monitor" onNavigate={onNavigate} actionLabel={t('data.clearSearch')} onAction={() => confirmClear(t('data.monitorLabel'), onClearMonitorResults)} />
    </div>}
  </section>;
}

function Metric({ icon, label, value }) {
  return <article><Icon name={icon} size={18} /><span>{label}</span><strong>{value}</strong></article>;
}

function DataCollection({ title, description, count, actionCount = count, icon, page, onNavigate, actionLabel, onAction }) {
  const { t } = useI18n();
  return <article className="data-collection"><div className="data-collection-icon"><Icon name={icon} size={19} /></div><div><h3>{title}</h3><p>{description}</p></div><div className="data-collection-actions"><strong>{count}</strong><span>{t('common.items')}</span><button type="button" className="data-open" onClick={() => onNavigate(page)}>{t('data.viewAll')}</button>{actionCount > 0 && <button type="button" onClick={onAction}>{actionLabel}</button>}</div></article>;
}
