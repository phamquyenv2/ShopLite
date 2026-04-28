import React, { useState } from 'react';
import { IonContent, IonPage, IonIcon, IonToast } from '@ionic/react';
import { arrowBack, storefrontOutline } from 'ionicons/icons';
import { useHistory, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import './SetStorePage.css';

const SetStorePage: React.FC = () => {
  const history = useHistory();
  const location = useLocation();
  const { setStoreName } = useAuth();

  const searchParams = new URLSearchParams(location.search);
  const sessionId = searchParams.get('sessionId') || '';
  const phone = searchParams.get('phone') || '';

  const [storeName, setStoreNameValue] = useState('');
  const [touched, setTouched] = useState(false);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const nameClean = storeName.trim();
  const isValid = nameClean.length >= 2 && nameClean.length <= 100;
  const canContinue = isValid && !busy;
  const showError = touched && !isValid;

  const handleContinue = async () => {
    if (!canContinue) return;
    setBusy(true);
    try {
      await setStoreName(sessionId, nameClean);
      history.push(
        `/register/complete?sessionId=${encodeURIComponent(sessionId)}&phone=${encodeURIComponent(phone)}`
      );
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể lưu tên cửa hàng.';
      setToast(msg);
    } finally {
      setBusy(false);
    }
  };

  return (
    <IonPage className="ss-page">
      <IonContent className="ss-content" scrollY={false}>

        {/* ── Header ── */}
        <div className="ss-header">
          <button className="ss-back-btn" onClick={() => history.goBack()} disabled={busy}>
            <IonIcon icon={arrowBack} />
          </button>
        </div>

        {/* ── Sheet ── */}
        <div className="ss-sheet">
          <div className="ss-icon-wrap">
            <IonIcon icon={storefrontOutline} className="ss-store-icon" />
          </div>

          <h1 className="ss-title">Đặt tên cửa hàng</h1>
          <p className="ss-subtitle">Tên sẽ xuất hiện trên hóa đơn và báo cáo của bạn</p>

          <div className={`ss-input-box ${showError ? 'error' : ''}`}>
            <input
              className="ss-input"
              type="text"
              placeholder="Ví dụ: Shop Hoa Mai, Cửa hàng ABC..."
              value={storeName}
              onChange={(e) => {
                setStoreNameValue(e.target.value);
                setTouched(true);
              }}
              disabled={busy}
              maxLength={100}
            />
          </div>

          {showError ? (
            <p className="ss-error-text">Tên cửa hàng phải từ 2 đến 100 ký tự</p>
          ) : (
            <p className="ss-error-text" style={{ visibility: 'hidden' }}>placeholder</p>
          )}

          <p className="ss-char-count">{nameClean.length}/100</p>
        </div>

        {/* ── Fixed footer ── */}
        <div className="ss-footer">
          <button
            className={`ss-continue-btn${canContinue ? ' active' : ''}`}
            disabled={!canContinue}
            onClick={handleContinue}
          >
            {busy ? 'Đang lưu…' : 'Tiếp tục'}
          </button>
        </div>

        <IonToast
          isOpen={toast !== null}
          message={toast ?? ''}
          duration={3000}
          onDidDismiss={() => setToast(null)}
        />

      </IonContent>
    </IonPage>
  );
};

export default SetStorePage;
