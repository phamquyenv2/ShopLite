import React, { useEffect, useRef } from 'react';
import { IonContent, IonPage } from '@ionic/react';
import {
  cartOutline,
  cubeOutline,
  peopleOutline,
  walletOutline,
  clipboardOutline,
  arrowForwardOutline,
  storefrontOutline,
  checkmarkCircle,
  shieldCheckmarkOutline,
  analyticsOutline,
  phonePortraitOutline,
} from 'ionicons/icons';
import { IonIcon } from '@ionic/react';
import { useHistory } from 'react-router-dom';
import './WelcomePage.css';

const features = [
  {
    icon: cartOutline,
    color: 'blue',
    title: 'Bán hàng nhanh',
    desc: 'Tạo đơn, thanh toán đa kênh (tiền mặt, chuyển khoản, ví điện tử) chỉ trong vài giây.',
  },
  {
    icon: cubeOutline,
    color: 'indigo',
    title: 'Quản lý hàng hoá',
    desc: 'Kiểm soát tồn kho, nhập hàng, kiểm kho và trả hàng nhập theo thời gian thực.',
  },
  {
    icon: peopleOutline,
    color: 'teal',
    title: 'Quản lý nhân viên',
    desc: 'Phân quyền chi tiết, theo dõi lịch làm việc và bảng lương cho từng nhân viên.',
  },
  {
    icon: walletOutline,
    color: 'green',
    title: 'Sổ quỹ & Tài chính',
    desc: 'Ghi nhận thu chi tự động, theo dõi số dư quỹ và lịch sử giao dịch đầy đủ.',
  },
  {
    icon: clipboardOutline,
    color: 'orange',
    title: 'Báo cáo & Phân tích',
    desc: 'Thống kê doanh thu, lợi nhuận và hiệu suất kinh doanh mọi lúc, mọi nơi.',
  },
  {
    icon: shieldCheckmarkOutline,
    color: 'purple',
    title: 'Bảo mật cao',
    desc: 'Hệ thống phân quyền chặt chẽ, đảm bảo dữ liệu cửa hàng luôn được bảo vệ.',
  },
];

const WelcomePage: React.FC = () => {
  const history = useHistory();
  const heroRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = heroRef.current;
    if (!el) return;
    el.classList.add('animated');
  }, []);

  return (
    <IonPage className="welcome-page">
      <IonContent className="welcome-content" scrollY>
        {/* ── Hero Section ── */}
        <div className="welcome-hero" ref={heroRef}>
          {/* Decorative blobs */}
          <div className="blob blob-1" />
          <div className="blob blob-2" />

          <div className="hero-inner">
            {/* Logo */}
            <div className="welcome-logo-wrap">
              <div className="welcome-logo-box">
                <IonIcon icon={storefrontOutline} />
              </div>
              <div className="logo-glow" />
            </div>

            <h1 className="welcome-brand">ShopLite</h1>
            <p className="welcome-tagline">
              Giải pháp quản lý cửa hàng <strong>toàn diện</strong>
              <br />cho doanh nghiệp vừa &amp; nhỏ
            </p>

            {/* ── CTA ngay trong hero ── */}
            <div className="hero-cta">
              <button className="hero-btn-primary" onClick={() => history.push('/login')}>
                Đăng nhập
                <IonIcon icon={arrowForwardOutline} />
              </button>
              <button className="hero-btn-ghost" onClick={() => history.push('/get-started')}>
                Bắt đầu miễn phí
              </button>
            </div>
          </div>
        </div>

        {/* ── Feature Cards ── */}
        <div className="welcome-body">
          <div className="feature-grid">
            {features.map((f, i) => (
              <div className="feature-card" key={i} style={{ animationDelay: `${i * 60}ms` }}>
                <div className={`feature-icon-box ${f.color}`}>
                  <IonIcon icon={f.icon} />
                </div>
                <div className="feature-text">
                  <h3>{f.title}</h3>
                  <p>{f.desc}</p>
                </div>
              </div>
            ))}
          </div>

          <p className="welcome-version">Phiên bản 1.0.0 · ShopLite © 2026</p>
        </div>
      </IonContent>
    </IonPage>
  );
};

export default WelcomePage;
