import {
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonToolbar,
} from '@ionic/react';
import {
  searchOutline,
  personCircleOutline,
  cashOutline,
  cartOutline,
  cubeOutline,
  peopleOutline,
  logInOutline,
  homeOutline,
  gridOutline,
  storefrontOutline,
  receiptOutline,
  reorderThreeOutline,
  callOutline,
  notificationsOutline,
  mailOutline,
  storefront
} from 'ionicons/icons';
import { useHistory } from 'react-router-dom';
import { useState } from 'react';
import { useIonViewWillEnter } from '@ionic/react';
import { authApis, endpoints } from '../utils/Apis';
import type { Order } from '../api/types';
import type { MeResponse } from '../auth/types';
import './Home.css';

const fmt = (n?: number | null) => (n ?? 0).toLocaleString('vi-VN');

const Home: React.FC = () => {
  const history = useHistory();

  const [todayRevenue, setTodayRevenue] = useState(0);
  const [todayOrdersCount, setTodayOrdersCount] = useState(0);
  const [todayProfit, setTodayProfit] = useState(0);
  const [recentOrders, setRecentOrders] = useState<Order[]>([]);
  const [storeName, setStoreName] = useState('');
  const [memberRole, setMemberRole] = useState('');

  useIonViewWillEnter(() => {
    const loadData = async () => {
      try {
        const meRes = await authApis().get<any>(endpoints.me);
        const mePayload = (meRes.data?.data ?? meRes.data) as MeResponse;
        const currentStore = mePayload?.currentStore ?? null;
        setStoreName(currentStore?.name || 'Chua co cua hang');
        setMemberRole(currentStore?.memberRole || '');

        const now = new Date();
        const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const params = new URLSearchParams();
        params.set('from', startOfDay.toISOString());
        params.set('to', now.toISOString());
        
        const url = `${endpoints.orders}?${params.toString()}`;
        const res = await authApis().get<any>(url);
        
        let orders: Order[] = [];
        if (Array.isArray(res.data?.data)) orders = res.data.data;
        else if (Array.isArray(res.data)) orders = res.data;

        const validOrders = orders.filter(o => {
          const isStatusValid = o.status === 'COMPLETED' || o.status === 'PAID' || o.status === 'PENDING_PAYMENT';
          if (!isStatusValid) return false;
          if (!o.createdAt) return false;
          const orderDate = new Date(o.createdAt);
          return orderDate >= startOfDay && orderDate <= now;
        });
        
        // Cũng lọc recentOrders theo ngày hôm nay
        const todayOrdersForRecent = orders.filter(o => {
          if (!o.createdAt) return false;
          const orderDate = new Date(o.createdAt);
          return orderDate >= startOfDay && orderDate <= now;
        });
        const rev = validOrders.reduce((sum, o) => sum + (o.totalAmount ?? 0), 0);
        const profit = validOrders.reduce((sum, o) => sum + ((o as any).profit ?? ((o.totalAmount ?? 0) * 0.3)), 0);

        setTodayRevenue(rev);
        setTodayOrdersCount(validOrders.length);
        setTodayProfit(profit);

        const sorted = [...todayOrdersForRecent].sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime());
        setRecentOrders(sorted.slice(0, 3));
      } catch (err) {
        console.error(err);
      }
    };
    loadData();
  });

  return (
    <IonPage className="home-page">
      <IonHeader className="ion-no-border home-header">
        <IonToolbar>
          <div className="header-container">
            <div className="header-left">
              <div className="logo-icon-box">
                <IonIcon icon={storefront} />
              </div>
              <span className="brand-name">{storeName || 'ShopLite'}</span>
              {memberRole ? <span className="sub-label">{memberRole}</span> : null}
            </div>
            <div className="header-right">
              <IonIcon icon={callOutline} className="header-action-icon" />
              <IonIcon icon={notificationsOutline} className="header-action-icon" />
              <IonIcon icon={mailOutline} className="header-action-icon" />
            </div>
          </div>
        </IonToolbar>
      </IonHeader>

      <IonContent className="home-content">
        <div className="home-container">
          {/* Thẻ doanh thu chính */}
          <div className="revenue-main-card">
          <div className="revenue-info">
            <p className="label1">DOANH THU HÔM NAY</p>
            <h2 className="amount">{fmt(todayRevenue)}đ</h2>
            <div className="trend-badge">
              <IonIcon icon={cashOutline} />
              <span>Cập nhật mới nhất</span>
            </div>
          </div>
          <IonIcon icon={cashOutline} className="bg-icon" />
        </div>

        <div className="stats-row">
          <div className="stats-card">
            <p className="label1">ĐƠN HÀNG</p>
            <h3 className="value">{todayOrdersCount}</h3>
            <p className="sub-label">Hoàn thành</p>
          </div>
          <div className="stats-card">
            <p className="label1">LỢI NHUẬN</p>
            <h3 className="value">{fmt(todayProfit)}đ</h3>
            <p className="sub-label warning">Tỷ suất ~30%</p>
          </div>
        </div>

        {/* Phím tắt nhanh */}
        <div className="section-container">
          <h4 className="section-title">Phím tắt nhanh</h4>
          <div className="shortcut-grid">
            <div className="shortcut-item">
              <div className="icon-box blue" onClick={() => history.push('/sales')}>
                <IonIcon icon={cartOutline} /></div>
              <span>Tạo đơn</span>
            </div>
            <div className="shortcut-item" onClick={() => history.push('/products')}>
              <div className="icon-box indigo"><IonIcon icon={cubeOutline} /></div>
              <span>Sản phẩm</span>
            </div>
            <div className="shortcut-item" onClick={() => history.push('/employees')}>
              <div className="icon-box orange"><IonIcon icon={peopleOutline} /></div>
              <span>Nhân viên</span>
            </div>
            <div className="shortcut-item" onClick={() => history.push('/import-orders')}>
              <div className="icon-box red"><IonIcon icon={logInOutline} /></div>
              <span>Nhập hàng</span>
            </div>
          </div>
        </div>

        {/* Đơn hàng mới */}
        <div className="section-container">
          <div className="section-header">
            <h4 className="section-title">Đơn hàng mới</h4>
            <span className="view-all" onClick={() => history.push('/orders')}>Xem tất cả</span>
          </div>

          {recentOrders.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '16px', color: '#6b7280', fontSize: '14px' }}>
              Chưa có đơn hàng nào
            </div>
          ) : recentOrders.map((o, idx) => {
            const timeStr = o.createdAt ? new Date(o.createdAt).toLocaleTimeString('vi-VN', {hour: '2-digit', minute:'2-digit'}) : '';
            const isCompleted = o.status === 'COMPLETED' || o.status === 'PAID';
            const statusColor = isCompleted ? 'blue' : o.status === 'PENDING_PAYMENT' ? 'orange' : 'red';
            const statusText = isCompleted ? 'Hoàn thành' : o.status === 'PENDING_PAYMENT' ? 'Chờ thanh toán' : 'Nháp';
            const itemsCount = o.items?.reduce((sum, i) => sum + (i.quantity ?? 1), 0) || 0;

            return (
              <div className="order-card" key={o.id || idx}>
                <div className={`order-user-icon ${statusColor}`}><IonIcon icon={personCircleOutline} /></div>
                <div className="order-info">
                  <div className="order-row">
                    <span className="name">{o.customerName || 'Khách lẻ'}</span>
                    <span className="amount">{fmt(o.totalAmount)}đ</span>
                  </div>
                  <div className="order-row">
                    <span className="detail">{timeStr} • {itemsCount} sản phẩm</span>
                    <span className={`status-badge ${statusColor}`}>{statusText}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

          <div className="bottom-spacer"></div>
        </div>
      </IonContent>

      {/* Thanh Tab giả lập theo ảnh */}
      <div className="custom-tab-bar">
        <div className="tab-item active" role="button" tabIndex={0}>
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
        <div className="tab-item" role="button" tabIndex={0} onClick={() => history.push('/more')} >
          <IonIcon icon={reorderThreeOutline} />
          <span>Khác</span>
        </div>
      </div>
    </IonPage>
  );
};

export default Home;
