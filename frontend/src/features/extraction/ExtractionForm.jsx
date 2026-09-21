import { Notice } from '../../components/ui/Notice';
import { useI18n } from '../../i18n/I18nContext';

export function ExtractionForm({ form, isLoading, notice, onChange, onSubmit }) {
  const { t } = useI18n();
  function submit(event) {
    event.preventDefault();
    onSubmit();
  }

  return <section className="extract-card"><div className="card-heading"><div><span className="step">01</span><h2>{t('extract.title')}</h2><p>{t('extract.description')}</p></div><span className="secure">● {t('extract.secure')}</span></div>
    <form onSubmit={submit}><label>{t('extract.link')}<div className="url-field"><span>⌁</span><input value={form.postUrl} onChange={(event) => onChange('postUrl', event.target.value)} placeholder={t('extract.placeholder')} required /></div></label>
      <div className="form-row"><label>{t('extract.max')}<input type="number" min="1" max="1000" value={form.maxComments} onChange={(event) => onChange('maxComments', event.target.value)} /></label><label className="toggle-row"><span>{t('extract.replies')}<small>{t('extract.repliesHelp')}</small></span><input type="checkbox" checked={form.includeReplies} onChange={(event) => onChange('includeReplies', event.target.checked)} /><i /></label><button className="primary-button" disabled={isLoading}>{isLoading ? t('extract.processing') : <>{t('extract.start')} <span>→</span></>}</button></div>
    </form><Notice notice={notice} />
  </section>;
}
