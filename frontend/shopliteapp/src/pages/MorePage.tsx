import {
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonToolbar,
} from '@ionic/react';
import {
  personCircleOutline,
  pencilOutline,
  chevronForwardOutline,
  bagHandleOutline,
  receiptOutline,
  bagOutline,
  returnUpBackOutline,
  walletOutline,
  archiveOutline,
  cartOutline,
  swapHorizontalOutline,
  logOutOutline,
  clipboardOutline,
  arrowUndoOutline,
  trashOutline,
  personOutline,
  storefrontOutline,
  peopleOutline,
  calendarOutline,
  pricetagOutline,
  timeOutline,
  documentTextOutline,
  settingsOutline,
  cashOutline,
  calculatorOutline,
  businessOutline,
  constructOutline,
  callOutline,
  chatbubbleEllipsesOutline,
  headsetOutline,
  globeOutline,
  informationCircleOutline,
  homeOutline,
  gridOutline,
  personAddOutline,
  reorderThreeOutline,
  appsOutline
} from 'ionicons/icons';
import { useHistory } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import './MorePage.css';

const MorePage: React.FC = () => {
  const history = useHistory();
  const { logout } = useAuth();

  const handleLogout = async () => {
    await logout();
    history.replace('/login');
  };

  return (
    <IonPage className="more-page">
      <IonHeader className="ion-no-border">
        <IonToolbar className="more-page-toolbar">
          {/* We optionally keep title empty since the picture doesn't have a title bar, just starts with the profile card */}
        </IonToolbar>
      </IonHeader>

      <IonContent className="more-page-content" color="light">
        <div className="more-container">

          {/* Profile Card */}
          <div className="profile-card">
            <div className="profile-header">
              <div className="profile-info-wrap">
                <div className="avatar-box">
                  <IonIcon icon={personCircleOutline} />
                </div>
                <div className="profile-details">
                  <h2>shoplite</h2>
                  <p>Chi nhánh trung tâm</p>
                </div>
              </div>
              <div className="edit-icon-box">
                <IonIcon icon={pencilOutline} />
              </div>
            </div>
            
            <div className="profile-footer">
              <span className="store-info-text">Thông tin cửa hàng</span>
              <div className="trial-badge">
                <IonIcon icon={chevronForwardOutline} />
              </div>
            </div>
          </div>

          {/* Giao dịch */}
          <div className="menu-group">
            <h3 className="group-title">Giao dịch</h3>
            <div className="grid-2-col">
              <div className="menu-grid-item" onClick={() => history.push('/sales')}>
                <IonIcon icon={bagHandleOutline} className="icon-blue" />
                <span>Bán hàng</span>
              </div>
              <div className="menu-grid-item" onClick={() => history.push('/orders')}>
                <IonIcon icon={receiptOutline} className="icon-blue" />
                <span>Hóa đơn</span>
              </div>
              <div className="menu-grid-item" onClick={() => history.push('/orders/new')}>
                <IonIcon icon={bagOutline} className="icon-blue" />
                <span>Đặt hàng</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={returnUpBackOutline} className="icon-blue" />
                <span>Trả hàng</span>
              </div>
              <div className="menu-grid-item" onClick={() => history.push('/fund-ledger')}>
                <IonIcon icon={walletOutline} className="icon-blue" />
                <span>Sổ quỹ</span>
              </div>
            </div>
          </div>

          {/* Hàng hoá */}
          <div className="menu-group">
            <h3 className="group-title">Hàng hoá</h3>
            <div className="grid-2-col">
              <div className="menu-grid-item" onClick={() => history.push('/products')}>
                <IonIcon icon={archiveOutline} className="icon-blue" />
                <span>Hàng hoá</span>
              </div>
              <div className="menu-grid-item" onClick={() => history.push('/inventory-adjustments')}>
                <IonIcon icon={clipboardOutline} className="icon-blue" />
                <span>Kiểm kho</span>
              </div>
              <div className="menu-grid-item" onClick={() => history.push('/import-orders')}>
                <IonIcon icon={cartOutline} className="icon-blue" />
                <span>Nhập hàng</span>
              </div>
              <div className="menu-grid-item" onClick={() => history.push('/import-return-orders')}>
                <IonIcon icon={arrowUndoOutline} className="icon-blue" />
                <span>Trả hàng nhập</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={swapHorizontalOutline} className="icon-green" />
                <span>Chuyển hàng</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={trashOutline} className="icon-blue" />
                <span>Xuất huỷ</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={logOutOutline} className="icon-blue" />
                <span>Xuất dùng nội bộ</span>
              </div>
            </div>
          </div>

          {/* Đối tác */}
          <div className="menu-group">
            <h3 className="group-title">Đối tác</h3>
            <div className="grid-2-col">
              <div className="menu-grid-item" onClick={() => history.push('/customers')}>
                <IonIcon icon={personOutline} className="icon-green" />
                <span>Khách hàng</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={storefrontOutline} className="icon-blue" />
                <span>Nhà cung cấp</span>
              </div>
            </div>
          </div>

          {/* Nhân viên */}
          <div className="menu-group">
            <h3 className="group-title">Nhân viên</h3>
            <div className="grid-2-col">
              <div className="menu-grid-item" onClick={() => history.push('/employees')}>
                <IonIcon icon={peopleOutline} className="icon-blue" />
                <span>Nhân viên</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={timeOutline} className="icon-blue" />
                <span>Lịch làm việc</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={calendarOutline} className="icon-blue" />
                <span>Chấm công</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={documentTextOutline} className="icon-blue" />
                <span>Bảng lương</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={pricetagOutline} className="icon-blue" />
                <span>Hoa hồng</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={settingsOutline} className="icon-blue" />
                <span>Thiết lập nhân viên</span>
              </div>
            </div>
          </div>

          {/* Báo cáo */}
          <div className="menu-group">
            <h3 className="group-title">Báo cáo</h3>
            <div className="grid-2-col">
              <div className="menu-grid-item">
                <IonIcon icon={calendarOutline} className="icon-blue" />
                <span>Cuối ngày</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={cashOutline} className="icon-green" />
                <span>Bán hàng</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={archiveOutline} className="icon-blue" />
                <span>Hàng hóa</span>
              </div>
            </div>
          </div>

          {/* Thuế & Kế toán */}
          <div className="menu-group">
            <h3 className="group-title">Thuế & Kế toán</h3>
            <div className="grid-2-col">
              <div className="menu-grid-item">
                <IonIcon icon={calculatorOutline} className="icon-green" />
                <span>Thuế & Kế toán</span>
              </div>
              <div className="menu-grid-item">
                <IonIcon icon={receiptOutline} className="icon-green" />
                <span>Hóa đơn điện tử</span>
              </div>
            </div>
          </div>

          {/* Cài đặt chung */}
          <div className="section-title-out">CÀI ĐẶT CHUNG</div>
          <div className="menu-list-group">
            <div className="menu-list-item">
              <div className="item-left">
                <IonIcon icon={settingsOutline} />
                <span>Thiết lập cửa hàng</span>
              </div>
              <IonIcon icon={chevronForwardOutline} className="chevron" />
            </div>
            <div className="menu-list-item">
              <div className="item-left">
                <IonIcon icon={constructOutline} />
                <span>Ứng dụng & thiết bị</span>
              </div>
              <IonIcon icon={chevronForwardOutline} className="chevron" />
            </div>
            <div className="menu-list-item">
              <div className="item-left">
                <IonIcon icon={personOutline} />
                <span>Quản lý người dùng</span>
              </div>
              <IonIcon icon={chevronForwardOutline} className="chevron" />
            </div>
          </div>

          {/* System Settings & Action */}
          <div className="menu-list-group">
            <div className="menu-list-item">
              <div className="item-left">
                <IonIcon icon={globeOutline} />
                <span>Ngôn ngữ</span>
              </div>
              <IonIcon icon={chevronForwardOutline} className="chevron" />
            </div>
            <div className="menu-list-item">
              <div className="item-left">
                <IonIcon icon={informationCircleOutline} />
                <span>Điều khoản sử dụng</span>
              </div>
              <IonIcon icon={chevronForwardOutline} className="chevron" />
            </div>
            <div className="menu-list-item text-danger" onClick={handleLogout}>
              <div className="item-left">
                <IonIcon icon={logOutOutline} />
                <span>Đăng xuất</span>
              </div>
            </div>
          </div>

          <div className="version-info">
            <p>Phiên bản 2.5.681</p>
          </div>

          <div className="bottom-spacer"></div>
        </div>
      </IonContent>

      {/* Thanh Tab giả lập theo ảnh */}
      <div className="custom-tab-bar">
        <div className="tab-item" role="button" tabIndex={0} onClick={() => history.push('/home')}>
          <IonIcon icon={homeOutline} />
          <span>Tổng quan</span>
        </div>
        <div className="tab-item" role="button" tabIndex={0} onClick={() => history.push('/products')}>
          <IonIcon icon={gridOutline} />
          <span>Hàng hóa</span>
        </div>
        <div className="tab-item" role="button" tabIndex={0} onClick={() => history.push('/sales')}>
          <IonIcon icon={storefrontOutline} />
          <span>Bán hàng</span>
        </div>
        <div className="tab-item" role="button" tabIndex={0} onClick={() => history.push('/orders')}>
          <IonIcon icon={receiptOutline} />
          <span>Hoá đơn</span>
        </div>
        <div className="tab-item active" role="button" tabIndex={0}>
          <IonIcon icon={reorderThreeOutline} />
          <span>Nhiều hơn</span>
        </div>
      </div>
    </IonPage>
  );
};

export default MorePage;
