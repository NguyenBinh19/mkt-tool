import { Icon } from '../../components/ui/Icon';
import { useI18n } from '../../i18n/I18nContext';

export function ToolCard({ tool, allowed, pending, onOpen, onRequest }) {
  const { t } = useI18n();
  const label = !tool.active ? t('tools.soon') : allowed ? t('tools.available') : pending ? t('tools.pending') : t('tools.permission');
  return <article className={`catalog-card ${tool.color}`}>
    <div className="catalog-card-top"><span className="catalog-icon"><Icon name={tool.icon} size={25} /></span><span className={`status-badge ${tool.active && allowed ? 'available' : ''}`}>{label}</span></div>
    <p className="catalog-category">{t(`tools.category.${tool.category}`)}</p><h3>{tool.labelKey ? t(tool.labelKey) : tool.label}</h3><p className="catalog-description">{t(tool.textKey)}</p>
    <div className="catalog-card-bottom"><small>{t(tool.detailKey)}</small>
      <button disabled={!tool.active || pending && !allowed} onClick={allowed ? onOpen : onRequest}>
        {!tool.active ? t('tools.developing') : allowed ? t('tools.open') : pending ? t('tools.requested') : t('tools.request')}<Icon name={allowed ? 'arrow' : 'shield'} size={16} />
      </button>
    </div>
  </article>;
}
