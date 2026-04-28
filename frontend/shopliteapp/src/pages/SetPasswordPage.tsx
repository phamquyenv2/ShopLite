import React, { useState } from 'react';
import { IonContent, IonPage, IonIcon, IonToast } from '@ionic/react';
import { arrowBack, eyeOutline, eyeOffOutline, lockClosedOutline } from 'ionicons/icons';
import { useHistory, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import './SetPasswordPage.css';

const SetPasswordPage: React.FC = () => {
  const history = useHistory();
  const location = useLocation();
  const { completeRegister } = useAuth();

  const searchParams = new URLSearchParams(location.search);
  const sessionId = searchParams.get('sessionId') || '';

  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [touched, setTouched] = useState({ password: false, confirm: false });
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const isPasswordValid = password.length >= 6;
  const isConfirmValid = confirmPassword === password && confirmPassword.length > 0;
  const canContinue = isPasswordValid && isConfirmValid && !busy;

  const showPwdError = touched.password && !isPasswordValid;
  const showConfirmError = touched.confirm && !isConfirmValid;

  const handleComplete = async () => {
    if (!canContinue) return;
    setBusy(true);
    try {
      await completeRegister(sessionId, password);
      // completeRegister sets authenticated → navigate home
      history.replace('/home');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Đăng ký thất bại. Vui lòng thử lại.';
      setToast(msg);
    } finally {
      setBusy(false);
    }
  };

  return (
    <IonPage className="sp-page">
      <IonContent className="sp-content" scrollY={false}>

        {/* ── Header ── */}
        <div className="sp-header">
          <button className="sp-back-btn" onClick={() => history.goBack()} disabled={busy}>
            <IonIcon icon={arrowBack} />
          </button>
        </div>

        {/* ── Sheet ── */}
        <div className="sp-sheet">
          <div className="sp-icon-wrap">
            <IonIcon icon={lockClosedOutline} className="sp-lock-icon" />
          </div>

          <h1 className="sp-title">Đặt mật khẩu</h1>
          <p className="sp-subtitle">Mật khẩu phải có ít nhất 6 ký tự</p>

          {/* Password */}
          <div className={`sp-input-box ${showPwdError ? 'error' : ''}`}>
            <input
              className="sp-input"
              type={showPassword ? 'text' : 'password'}
              placeholder="Mật khẩu"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setTouched(t => ({ ...t, password: true }));
              }}
              disabled={busy}
            />
            <button
              type="button"
              className="sp-eye-btn"
              onClick={() => setShowPassword(v => !v)}
              disabled={busy}
            >
              <IonIcon icon={showPassword ? eyeOffOutline : eyeOutline} />
            </button>
          </div>
          {showPwdError ? (
            <p className="sp-error-text">Mật khẩu phải có ít nhất 6 ký tự</p>
          ) : (
            <p className="sp-error-text" style={{ visibility: 'hidden' }}>placeholder</p>
          )}

          {/* Confirm Password */}
          <div className={`sp-input-box ${showConfirmError ? 'error' : ''}`}>
            <input
              className="sp-input"
              type={showConfirm ? 'text' : 'password'}
              placeholder="Nhập lại mật khẩu"
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                setTouched(t => ({ ...t, confirm: true }));
              }}
              disabled={busy}
              onKeyDown={(e) => e.key === 'Enter' && void handleComplete()}
            />
            <button
              type="button"
              className="sp-eye-btn"
              onClick={() => setShowConfirm(v => !v)}
              disabled={busy}
            >
              <IonIcon icon={showConfirm ? eyeOffOutline : eyeOutline} />
            </button>
          </div>
          {showConfirmError ? (
            <p className="sp-error-text">Mật khẩu nhập lại không khớp</p>
          ) : (
            <p className="sp-error-text" style={{ visibility: 'hidden' }}>placeholder</p>
          )}
        </div>

        {/* ── Footer ── */}
        <div className="sp-footer">
          <button
            className={`sp-complete-btn${canContinue ? ' active' : ''}`}
            disabled={!canContinue}
            onClick={handleComplete}
          >
            {busy ? 'Đang tạo tài khoản…' : 'Hoàn tất đăng ký'}
          </button>
        </div>

        <IonToast
          isOpen={toast !== null}
          message={toast ?? ''}
          duration={3500}
          onDidDismiss={() => setToast(null)}
        />

      </IonContent>
    </IonPage>
  );
};

export default SetPasswordPage;
