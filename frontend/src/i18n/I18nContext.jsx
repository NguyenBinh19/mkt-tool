import { createContext, useContext, useEffect, useMemo } from 'react';
import { translations } from './translations';

const I18nContext = createContext(null);

export function I18nProvider({ language, onLanguageChange, children }) {
  useEffect(() => { document.documentElement.lang = language; }, [language]);
  const value = useMemo(() => ({
    language,
    locale: language === 'en' ? 'en-US' : 'vi-VN',
    setLanguage: onLanguageChange,
    t(key, variables = {}) {
      const template = translations[language]?.[key] ?? translations.vi[key] ?? key;
      return Object.entries(variables).reduce((text, [name, replacement]) => text.replaceAll(`{${name}}`, String(replacement)), template);
    }
  }), [language, onLanguageChange]);
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) throw new Error('useI18n must be used inside I18nProvider');
  return context;
}
