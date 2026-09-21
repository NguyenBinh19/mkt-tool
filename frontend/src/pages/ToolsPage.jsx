import { useState } from 'react';
import { Icon } from '../components/ui/Icon';
import { ToolCard } from '../features/tools/ToolCard';
import { toolCatalog } from '../features/tools/catalog';
import { useI18n } from '../i18n/I18nContext';

const categoryIds = ['all', ...new Set(toolCatalog.map((tool) => tool.category))];

export function ToolsPage({ user, onOpenTool, onRequestTool, canUse, requested, resultCount, leadCount, historyCount }) {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('all');
  const { t } = useI18n();
  const categoryLabel = (id) => id === 'all' ? t('tools.all') : t(`tools.category.${id}`);
  const visible = toolCatalog.filter((tool) => (category === 'all' || tool.category === category) && `${tool.label ?? ''} ${tool.labelKey ? t(tool.labelKey) : ''} ${t(tool.textKey)}`.toLocaleLowerCase().includes(query.toLocaleLowerCase()));

  return <section className="catalog-page">
    <div className="welcome-banner">
      <div className="welcome-copy"><p className="welcome-kicker">{t('tools.greeting', { name: user.name.toLocaleUpperCase() })}</p>
        <h2>{t('tools.heroTitle1')}<br /><span>{t('tools.heroTitle2')}</span></h2>
        <p>{t('tools.heroText')}</p>
        <a className="welcome-cta" href="#tool-catalog">{t('tools.explore')} <Icon name="arrow" size={18} /></a>
      </div>
      <div className="workflow-art" aria-hidden="true">
        <div className="art-orbit" /><div className="art-orbit inner" />
        <div className="art-center"><span>↗</span><small>leadflow</small></div>
        <div className="art-node art-source"><Icon name="chat" /><span>{t('tools.conversation')}</span></div>
        <div className="art-node art-lead"><Icon name="target" /><span>{t('tools.customer')}</span></div>
        <div className="art-node art-growth"><Icon name="chart" /><span>{t('tools.growth')}</span></div>
        <span className="art-dot dot-one" /><span className="art-dot dot-two" />
      </div>
    </div>
    <div className="overview-stats">
      <div><Icon name="chat" /><span>{t('tools.sessionComments')}</span><strong>{resultCount}</strong></div>
      <div><Icon name="users" /><span>{t('tools.savedLeads')}</span><strong>{leadCount}</strong></div>
      <div><Icon name="clock" /><span>{t('tools.historySessions')}</span><strong>{historyCount}</strong></div>
    </div>
    <section id="tool-catalog" className="catalog-section">
      <div className="catalog-heading"><div><p className="eyebrow">{t('tools.yourTools')}</p><h2>{t('tools.more')}</h2></div><label className="catalog-search"><Icon name="search" size={18} /><input aria-label={t('tools.search')} placeholder={t('tools.search')} value={query} onChange={(event) => setQuery(event.target.value)} /></label></div>
      <div className="catalog-filters">{categoryIds.map((item) => <button key={item} aria-pressed={category === item} onClick={() => setCategory(item)}>{categoryLabel(item)}</button>)}</div>
      <div className="catalog-grid">{visible.map((tool) => <ToolCard key={tool.id} tool={tool} allowed={canUse(tool.id)} pending={requested(tool.id)} onOpen={() => onOpenTool(tool.id)} onRequest={() => onRequestTool(tool.id)} />)}</div>
      {!visible.length && <div className="approval-empty"><span><Icon name="search" /></span><h3>{t('tools.noResult')}</h3><p>{t('tools.noResultText')}</p><button className="ghost-button" onClick={() => { setQuery(''); setCategory('all'); }}>{t('tools.reset')}</button></div>}
    </section>
    <div className="workflow-note"><Icon name="target" /><div><strong>{t('tools.workflowTitle')}</strong><p>{t('tools.workflowText')}</p></div><span>01 — 02 — 03</span></div>
  </section>;
}
