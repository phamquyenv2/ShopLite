import React, { useState } from 'react';
import { IonContent, IonPage, IonIcon, IonToast } from '@ionic/react';
import { arrowBack, chevronDown } from 'ionicons/icons';
import { useHistory } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import './GetStartedPage.css';

const GetStartedPage: React.FC = () => {
  const history = useHistory();
  const { sendOtp } = useAuth();

  const [phone, setPhone] = useState('');
  const [agreed, setAgreed] = useState(false);
  const [touched, setTouched] = useState(false);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const isPhoneValid = /^0\d{9}$/.test(phone);
  const canContinue = isPhoneValid && agreed && !busy;

  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value.replace(/\D/g, '').slice(0, 10);
    setPhone(raw);
    setTouched(true);
  };

  const showError = touched && !isPhoneValid;

  const handleContinue = async () => {
    if (!canContinue) return;
    setBusy(true);
    try {
      await sendOtp(phone);
      history.push(`/otp?phone=${encodeURIComponent(phone)}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể gửi OTP. Vui lòng thử lại.';
      setToast(msg);
    } finally {
      setBusy(false);
    }
  };

  return (
    <IonPage className="gs-page">
      <IonContent className="gs-content" scrollY={false}>

        {/* ── Gradient header ── */}
        <div className="gs-header">
          <button className="gs-back-btn" onClick={() => history.goBack()}>
            <IonIcon icon={arrowBack} />
          </button>
        </div>

        {/* ── White sheet card ── */}
        <div className="gs-sheet">

          <h1 className="gs-title">Nhập số điện thoại</h1>

          {/* Phone input */}
          <div className={`gs-input-box ${showError ? 'error' : ''}`}>
            <input
              className="gs-phone-input"
              type="tel"
              inputMode="numeric"
              placeholder="0912 345 678"
              value={phone}
              onChange={handlePhoneChange}
              disabled={busy}
            />
          </div>

          {showError ? (
            <p className="gs-error-text">Vui lòng nhập số điện thoại hợp lệ (10 số)</p>
          ) : (
            <p className="gs-error-text" style={{ visibility: 'hidden' }}>placeholder</p>
          )}

          {/* Terms checkbox */}
          <label className="gs-terms-row">
            <input
              type="checkbox"
              className="gs-native-check"
              checked={agreed}
              onChange={(e) => setAgreed(e.target.checked)}
              disabled={busy}
            />
            <div className="gs-custom-checkbox"></div>
            <span className="gs-terms-text">
              Tôi đồng ý với{' '}
              <a className="gs-link" href="#terms">Điều khoản và Điều kiện sử dụng</a>
              {' '}của ứng dụng ShopLite
            </span>
          </label>

          {/* Other method */}
          <button className="gs-other-method" disabled={busy}>
            Thêm phương thức khác
            <IonIcon icon={chevronDown} />
          </button>

        </div>

        {/* ── Fixed footer button ── */}
        <div className="gs-footer">
          <button
            className={`gs-continue-btn${canContinue ? ' active' : ''}`}
            disabled={!canContinue}
            onClick={handleContinue}
          >
            {busy ? 'Đang gửi OTP…' : 'Tiếp tục'}
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

export default GetStartedPage;
