import React, { useState, useEffect, useRef } from 'react';
import { IonContent, IonPage, IonIcon, IonToast } from '@ionic/react';
import { arrowBack } from 'ionicons/icons';
import { useHistory, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { FCM_OTP_EVENT, type FcmOtpEventDetail } from '../utils/fcmOtp';
import './OtpPage.css';

const OtpPage: React.FC = () => {
  const history = useHistory();
  const location = useLocation();
  const { verifyOtp, sendOtp } = useAuth();

  const searchParams = new URLSearchParams(location.search);
  const phone = searchParams.get('phone') || '';

  // 6 ô OTP
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const ref0 = useRef<HTMLInputElement>(null);
  const ref1 = useRef<HTMLInputElement>(null);
  const ref2 = useRef<HTMLInputElement>(null);
  const ref3 = useRef<HTMLInputElement>(null);
  const ref4 = useRef<HTMLInputElement>(null);
  const ref5 = useRef<HTMLInputElement>(null);
  const inputRefs = [ref0, ref1, ref2, ref3, ref4, ref5];

  const [countdown, setCountdown] = useState(60);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    let timer: ReturnType<typeof setInterval>;
    if (countdown > 0) {
      timer = setInterval(() => setCountdown(c => c - 1), 1000);
    }
    return () => clearInterval(timer);
  }, [countdown]);

  useEffect(() => {
    const fillOtp = (value: string) => {
      const digits = value.replace(/\D/g, '').slice(0, 6).split('');
      if (digits.length !== 6) return;
      setOtp(digits);
      setToast('Đã nhận OTP từ thông báo');
      void handleVerify(digits.join(''));
    };

    const onFcmOtp = (event: Event) => {
      const detail = (event as CustomEvent<FcmOtpEventDetail>).detail;
      if (!detail?.otp) return;
      fillOtp(detail.otp);
    };

    globalThis.addEventListener(FCM_OTP_EVENT, onFcmOtp);

    try {
      const raw = sessionStorage.getItem('shoplite:last-fcm-otp');
      if (raw) {
        sessionStorage.removeItem('shoplite:last-fcm-otp');
        const detail = JSON.parse(raw) as FcmOtpEventDetail;
        if (detail?.otp) fillOtp(detail.otp);
      }
    } catch {
      // ignore
    }

    return () => globalThis.removeEventListener(FCM_OTP_EVENT, onFcmOtp);
    // handleVerify intentionally stays outside dependencies to avoid re-subscription while typing.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleChange = (index: number, e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, '').slice(-1);
    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);

    // Auto focus next
    if (value && index < 5) {
      inputRefs[index + 1].current?.focus();
    }

    // Auto submit khi đủ 6 số
    if (index === 5 && value && newOtp.slice(0, 5).every(x => x !== '')) {
      const fullOtp = [...newOtp.slice(0, 5), value].join('');
      void handleVerify(fullOtp);
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs[index - 1].current?.focus();
    }
  };

  const handleVerify = async (fullOtp: string) => {
    if (busy) return;
    setBusy(true);
    try {
      const result = await verifyOtp(phone, fullOtp);
      const sessionId = result?.registerSessionId;
      history.push(`/register/store?sessionId=${encodeURIComponent(sessionId)}&phone=${encodeURIComponent(phone)}`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'OTP không đúng. Vui lòng thử lại.';
      setToast(msg);
      // Xóa hết ô và focus ô đầu
      setOtp(['', '', '', '', '', '']);
      setTimeout(() => inputRefs[0].current?.focus(), 100);
    } finally {
      setBusy(false);
    }
  };

  const handleResend = async () => {
    if (countdown > 0 || busy) return;
    setBusy(true);
    try {
      await sendOtp(phone);
      setCountdown(60);
      setOtp(['', '', '', '', '', '']);
      setToast('Đã gửi lại OTP!');
      setTimeout(() => inputRefs[0].current?.focus(), 100);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể gửi lại OTP.';
      setToast(msg);
    } finally {
      setBusy(false);
    }
  };

  // Format phone để hiển thị: 0912345678 → 0912 345 678
  const formatPhone = (p: string) => {
    if (!p) return '';
    const d = p.replace(/\D/g, '');
    if (d.length === 10) return `${d.slice(0, 4)} ${d.slice(4, 7)} ${d.slice(7)}`;
    return p;
  };

  return (
    <IonPage className="otp-page">
      <IonContent className="otp-content" scrollY={false}>

        {/* ── Gradient header ── */}
        <div className="otp-header">
          <button className="otp-back-btn" onClick={() => history.goBack()} disabled={busy}>
            <IonIcon icon={arrowBack} />
          </button>
        </div>

        {/* ── White sheet card ── */}
        <div className="otp-sheet">
          <h1 className="otp-title">Nhập mã xác thực</h1>

          <p className="otp-desc">
            Mã đã gửi đến số <strong className="otp-phone-text">{formatPhone(phone)}</strong>
          </p>

          <button className="otp-change-phone" onClick={() => history.goBack()} disabled={busy}>
            Đổi số điện thoại
          </button>

          {/* 6 ô OTP */}
          <div className="otp-input-group">
            {otp.map((digit, index) => (
              <input
                key={index}
                ref={inputRefs[index]}
                type="tel"
                inputMode="numeric"
                className={`otp-digit-input ${digit ? 'filled' : ''} ${busy ? 'otp-disabled' : ''}`}
                value={digit}
                onChange={(e) => handleChange(index, e)}
                onKeyDown={(e) => handleKeyDown(index, e)}
                autoFocus={index === 0}
                disabled={busy}
                maxLength={1}
              />
            ))}
          </div>

          {/* Resend */}
          <p className="otp-resend-text">
            {countdown > 0 ? (
              <>Gửi lại sau <span className="otp-timer">{countdown}s</span></>
            ) : (
              <button
                className="otp-resend-btn"
                onClick={handleResend}
                disabled={busy}
              >
                Gửi lại OTP
              </button>
            )}
          </p>
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

export default OtpPage;
