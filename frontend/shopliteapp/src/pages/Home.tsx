import {
  IonButton,
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonToolbar,
  IonBadge,
  IonFab,
  IonFabButton,
} from '@ionic/react';
import {
  searchOutline,
  personCircleOutline,
  cashOutline,
  cartOutline,
  cubeOutline,
  peopleOutline,
  logInOutline,
  addOutline,
  homeOutline,
  gridOutline,
  storefrontOutline,
  personAddOutline,
  reorderThreeOutline
} from 'ionicons/icons';
import './Home.css';

const Home: React.FC = () => {
  return (
    <IonPage>
      <IonHeader className="ion-no-border home-header">
        <IonToolbar>
          <div className="header-container">
            <IonIcon icon={personCircleOutline} className="user-avatar" />
            <div className="header-title">
              <h1>Minimart</h1>
            </div>
            <IonIcon icon={searchOutline} className="search-icon" />
          </div>
        </IonToolbar>
      </IonHeader>

      <IonContent className="home-content">
        {/* Thẻ doanh thu chính */}
        <div className="revenue-main-card">
          <div className="revenue-info">
            <p className="label1">DOANH THU HÔM NAY</p>
            <h2 className="amount">12.450.000đ</h2>
            <div className="trend-badge">
              <IonIcon icon={cashOutline} />
              <span>+12% so với hôm qua</span>
            </div>
          </div>
          <IonIcon icon={cashOutline} className="bg-icon" />
        </div>

        <div className="stats-row">
          <div className="stats-card">
            <p className="label1">ĐƠN HÀNG</p>
            <h3 className="value">48</h3>
            <p className="sub-label">Hoàn thành 42</p>
          </div>
          <div className="stats-card">
            <p className="label1">LỢI NHUẬN</p>
            <h3 className="value">3.240.000đ</h3>
            <p className="sub-label warning">Tỷ suất 26%</p>
          </div>
        </div>

        {/* Phím tắt nhanh */}
        <div className="section-container">
          <h4 className="section-title">Phím tắt nhanh</h4>
          <div className="shortcut-grid">
            <div className="shortcut-item">
              <div className="icon-box blue"><IonIcon icon={cartOutline} /></div>
              <span>Tạo đơn</span>
            </div>
            <div className="shortcut-item">
              <div className="icon-box indigo"><IonIcon icon={cubeOutline} /></div>
              <span>Sản phẩm</span>
            </div>
            <div className="shortcut-item">
              <div className="icon-box orange"><IonIcon icon={peopleOutline} /></div>
              <span>Nhân viên</span>
            </div>
            <div className="shortcut-item">
              <div className="icon-box red"><IonIcon icon={logInOutline} /></div>
              <span>Nhập hàng</span>
            </div>
          </div>
        </div>

        {/* Đơn hàng mới */}
        <div className="section-container">
          <div className="section-header">
            <h4 className="section-title">Đơn hàng mới</h4>
            <span className="view-all">Xem tất cả</span>
          </div>

          <div className="order-card">
            <div className="order-user-icon blue"><IonIcon icon={personCircleOutline} /></div>
            <div className="order-info">
              <div className="order-row">
                <span className="name">Anh Hoàng (Minimart)</span>
                <span className="amount">540.000đ</span>
              </div>
              <div className="order-row">
                <span className="detail">2 phút trước • 3 sản phẩm</span>
                <span className="status-badge blue">Chờ giao</span>
              </div>
            </div>
          </div>

          <div className="order-card">
            <div className="order-user-icon orange"><IonIcon icon={personCircleOutline} /></div>
            <div className="order-info">
              <div className="order-row">
                <span className="name">Chị Lan Tây Hồ</span>
                <span className="amount">1.200.000đ</span>
              </div>
              <div className="order-row">
                <span className="detail">15 phút trước • 1 sản phẩm</span>
                <span className="status-badge orange">CHỜ XỬ LÝ</span>
              </div>
            </div>
          </div>
        </div>

        <div className="bottom-spacer"></div>
      </IonContent>

      {/* Thanh Tab giả lập theo ảnh */}
      <div className="custom-tab-bar">
        <div className="tab-item active">
          <IonIcon icon={homeOutline} />
          <span>Tổng quan</span>
        </div>
        <div className="tab-item">
          <IonIcon icon={gridOutline} />
          <span>Hàng hóa</span>
        </div>
        <div className="tab-item">
          <IonIcon icon={storefrontOutline} />
          <span>Bán hàng</span>
        </div>
        <div className="tab-item">
          <IonIcon icon={personAddOutline} />
          <span>Đối tác</span>
        </div>
        <div className="tab-item">
          <IonIcon icon={reorderThreeOutline} />
          <span>Khác</span>
        </div>
      </div>
    </IonPage>
  );
};

export default Home;