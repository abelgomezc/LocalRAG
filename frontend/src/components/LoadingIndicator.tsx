import { useApp } from '../context/AppContext';

export function LoadingIndicator() {
  const { t } = useApp();

  return (
    <div className="loading">
      <div className="loading-spinner" />
      {t('searching')}
    </div>
  );
}
