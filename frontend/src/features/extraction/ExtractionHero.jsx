import { useI18n } from '../../i18n/I18nContext';
export function ExtractionHero() {
  const { t } = useI18n();
  return <section className="hero"><div className="hero-copy"><span className="pill">{t('common.facebookIntelligence')}</span><h2>{t('extract.heroTitle1')}<br /><em>{t('extract.heroTitle2')}</em></h2><p>{t('extract.heroText')}</p></div><div className="hero-orbit"><div className="orbit-ring" /><div className="orbit-center">◎</div><span className="orbit-tag tag-one">{t('extract.comments')}</span><span className="orbit-tag tag-two">{t('extract.customers')}</span><span className="spark">✦</span></div></section>;
}
