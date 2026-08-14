import { createContext, useContext, useState, ReactNode } from 'react';
import { translations, TranslationKey } from '../i18n/translations';

type Language = 'es' | 'en';
type Theme = 'light' | 'dark';

interface AppContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  theme: Theme;
  setTheme: (theme: Theme) => void;
  t: (key: TranslationKey, params?: string[]) => string;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export function AppProvider({ children }: { children: ReactNode }) {
  const [language, setLanguage] = useState<Language>('es');
  const [theme, setTheme] = useState<Theme>('light');

  const t = (key: TranslationKey, params?: string[]): string => {
    let text = translations[language][key] || key;
    if (params) {
      params.forEach((param, index) => {
        text = text.replace(`{${index}}`, param);
      });
    }
    return text;
  };

  return (
    <AppContext.Provider value={{ language, setLanguage, theme, setTheme, t }}>
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
}
