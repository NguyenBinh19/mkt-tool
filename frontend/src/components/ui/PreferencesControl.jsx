import { useI18n } from '../../i18n/I18nContext';

export function PreferencesControl({ theme, onThemeChange, compact = false }) {
  const { language, setLanguage, t } = useI18n();
  const nextTheme = theme === 'dark' ? 'light' : 'dark';

  return <div className={`preference-controls${compact ? ' compact' : ''}`}>
    <div className="language-switcher" role="group" aria-label={t('common.language')}>
      <button type="button" aria-pressed={language === 'vi'} className={language === 'vi' ? 'active' : ''} onClick={() => setLanguage('vi')}>VI</button>
      <button type="button" aria-pressed={language === 'en'} className={language === 'en' ? 'active' : ''} onClick={() => setLanguage('en')}>EN</button>
    </div>
    <button className="theme-switcher" type="button" aria-label={t('common.appearance')} aria-pressed={theme === 'light'} onClick={() => onThemeChange(nextTheme)}>
      {theme === 'dark' ? `☀ ${t('common.light')}` : `◐ ${t('common.dark')}`}
    </button>
  </div>;
}
