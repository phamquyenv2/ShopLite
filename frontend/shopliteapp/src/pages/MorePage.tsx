import {
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonToolbar,
  useIonViewWillEnter,
} from '@ionic/react';
import {
  archiveOutline,
  arrowUndoOutline,
  bagHandleOutline,
  bagOutline,
  calendarOutline,
  cartOutline,
  chevronForwardOutline,
  clipboardOutline,
  documentTextOutline,
  gridOutline,
  homeOutline,
  informationCircleOutline,
  logOutOutline,
  peopleOutline,
  personCircleOutline,
  personOutline,
  receiptOutline,
  reorderThreeOutline,
  storefrontOutline,
  timeOutline,
  walletOutline,
} from 'ionicons/icons';
import { useState } from 'react';
import { useHistory } from 'react-router-dom';
import type { Menu, Permission } from '../api/types';
import type { MeResponse } from '../auth/types';
import { useAuth } from '../auth/useAuth';
import { authApis, endpoints } from '../utils/Apis';
import { canShowMenu, getMenuIcon, getMenusByType, getMenuTitle, hasMenuPayload } from '../utils/menuAccess';
import './MorePage.css';

type StaticItem = {
  code: string;
  label: string;
  route?: string;
  icon: string;
  iconClass: string;
  apiPath: string;
  method?: string;
};

type StaticGroup = {
  title: string;
  items: StaticItem[];
};

const iconClassByCode: Record<string, string> = {
  ITEM_CUSTOMERS: 'icon-green',
  ITEM_FUND_LEDGER: 'icon-blue',
  ITEM_PRODUCTS: 'icon-blue',
};

const fallbackTabs = (permissions: Permission[], menus: Menu[]) => [
  { label: 'Tổng quan', icon: homeOutline, route: '/home', show: true },
  { label: 'Hàng hóa', icon: gridOutline, route: '/products', show: canShowMenu(menus, 'MENU_PRODUCTS', permissions, '/api/v1/products') },
  { label: 'Bán hàng', icon: storefrontOutline, route: '/sales', show: canShowMenu(menus, 'MENU_SALES', permissions, '/api/v1/orders', 'POST') },
  { label: 'Hoá đơn', icon: receiptOutline, route: '/orders', show: canShowMenu(menus, 'MENU_ORDERS', permissions, '/api/v1/orders') },
  { label: 'Nhiều hơn', icon: reorderThreeOutline, route: '/more', show: true },
].filter(item => item.show);

const fallbackGroups: StaticGroup[] = [
  {
    title: 'Giao dịch',
    items: [
      { code: 'ITEM_SALES', label: 'Bán hàng', route: '/sales', icon: bagHandleOutline, iconClass: 'icon-blue', apiPath: '/api/v1/orders', method: 'POST' },
      { code: 'ITEM_ORDERS', label: 'Hóa đơn', route: '/orders', icon: receiptOutline, iconClass: 'icon-blue', apiPath: '/api/v1/orders' },
      { code: 'ITEM_ORDER_CREATE', label: 'Đặt hàng', route: '/orders/new', icon: bagOutline, iconClass: 'icon-blue', apiPath: '/api/v1/orders', method: 'POST' },
      { code: 'ITEM_FUND_LEDGER', label: 'Sổ quỹ', route: '/fund-ledger', icon: walletOutline, iconClass: 'icon-blue', apiPath: '/api/v1/fund-accounts' },
    ],
  },
  {
    title: 'Hàng hoá',
    items: [
      { code: 'ITEM_PRODUCTS', label: 'Hàng hoá', route: '/products', icon: archiveOutline, iconClass: 'icon-blue', apiPath: '/api/v1/products' },
      { code: 'ITEM_INVENTORY_ADJUSTMENTS', label: 'Kiểm kho', route: '/inventory-adjustments', icon: clipboardOutline, iconClass: 'icon-blue', apiPath: '/api/v1/inventory-adjustments' },
      { code: 'ITEM_IMPORT_ORDERS', label: 'Nhập hàng', route: '/import-orders', icon: cartOutline, iconClass: 'icon-blue', apiPath: '/api/v1/import-orders' },
      { code: 'ITEM_IMPORT_RETURN_ORDERS', label: 'Trả hàng nhập', route: '/import-return-orders', icon: arrowUndoOutline, iconClass: 'icon-blue', apiPath: '/api/v1/import-return-orders' },
    ],
  },
  {
    title: 'Đối tác',
    items: [
      { code: 'ITEM_CUSTOMERS', label: 'Khách hàng', route: '/customers', icon: personOutline, iconClass: 'icon-green', apiPath: '/api/v1/customers' },
    ],
  },
  {
    title: 'Nhân viên',
    items: [
      { code: 'ITEM_EMPLOYEES', label: 'Nhân viên', route: '/employees', icon: peopleOutline, iconClass: 'icon-blue', apiPath: '/api/v1/employees' },
      { code: 'ITEM_ROSTER', label: 'Lịch làm việc', icon: timeOutline, iconClass: 'icon-blue', apiPath: '/api/v1/roster/day' },
      { code: 'ITEM_ATTENDANCE', label: 'Chấm công', icon: calendarOutline, iconClass: 'icon-blue', apiPath: '/api/v1/attendance' },
      { code: 'ITEM_PAYROLLS', label: 'Bảng lương', icon: documentTextOutline, iconClass: 'icon-blue', apiPath: '/api/v1/payrolls' },
    ],
  },
];

const MorePage: React.FC = () => {
  const history = useHistory();
  const { logout } = useAuth();
  const [storeName, setStoreName] = useState('ShopLite');
  const [memberRole, setMemberRole] = useState('');
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [menus, setMenus] = useState<Menu[]>([]);

  useIonViewWillEnter(() => {
    const loadMe = async () => {
      try {
        const meRes = await authApis().get<any>(endpoints.me);
        const mePayload = (meRes.data?.data ?? meRes.data) as MeResponse;
        const currentStore = mePayload?.currentStore ?? null;
        setStoreName(currentStore?.name || 'ShopLite');
        setMemberRole(currentStore?.memberRole || '');
        setPermissions((currentStore?.permissions || []) as Permission[]);
        setMenus((currentStore?.menus || []) as Menu[]);
      } catch (err) {
        console.error(err);
      }
    };
    loadMe();
  });

  const handleLogout = async () => {
    await logout();
    history.replace('/login');
  };

  const menuGroups = hasMenuPayload(menus)
    ? getMenusByType(menus, 'GROUP')
        .map(group => ({
          title: getMenuTitle(group),
          items: getMenusByType(menus, 'ITEM')
            .filter(item => item.parentId === group.id)
            .map(item => ({
              code: item.code,
              label: getMenuTitle(item),
              route: item.route || undefined,
              icon: getMenuIcon(item.icon, archiveOutline),
              iconClass: iconClassByCode[item.code] || 'icon-blue',
              apiPath: '',
            })),
        }))
        .filter(group => group.items.length > 0)
    : fallbackGroups
        .map(group => ({
          ...group,
          items: group.items.filter(item => canShowMenu(menus, item.code, permissions, item.apiPath, item.method)),
        }))
        .filter(group => group.items.length > 0);

  const tabs = hasMenuPayload(menus)
    ? getMenusByType(menus, 'TAB')
        .filter(item => item.route)
        .map(item => ({
          label: item.code === 'MENU_MORE' ? 'Nhiều hơn' : getMenuTitle(item),
          icon: getMenuIcon(item.icon, homeOutline),
          route: item.route || '/home',
        }))
    : fallbackTabs(permissions, menus);

  return (
    <IonPage className="more-page">
      <IonHeader className="ion-no-border">
        <IonToolbar className="more-page-toolbar" />
      </IonHeader>

      <IonContent className="more-page-content" color="light">
        <div className="more-container">
          <div className="profile-card">
            <div className="profile-header">
              <div className="profile-info-wrap">
                <div className="avatar-box">
                  <IonIcon icon={personCircleOutline} />
                </div>
                <div className="profile-details">
                  <h2>{storeName}</h2>
                  <p>{memberRole || 'Thành viên'}</p>
                </div>
              </div>
            </div>

            <div className="profile-footer">
              <span className="store-info-text">Thông tin cửa hàng</span>
              <div className="trial-badge">
                <IonIcon icon={chevronForwardOutline} />
              </div>
            </div>
          </div>

          {menuGroups.map(group => (
            <div className="menu-group" key={group.title}>
              <h3 className="group-title">{group.title}</h3>
              <div className="grid-2-col">
                {group.items.map(item => (
                  <div
                    className="menu-grid-item"
                    key={item.code}
                    onClick={() => item.route && history.push(item.route)}
                  >
                    <IonIcon icon={item.icon} className={item.iconClass} />
                    <span>{item.label}</span>
                  </div>
                ))}
              </div>
            </div>
          ))}

          <div className="menu-list-group">
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

      <div className="custom-tab-bar">
        {tabs.map(item => (
          <div
            className={`tab-item ${item.route === '/more' ? 'active' : ''}`}
            key={item.route}
            role="button"
            tabIndex={0}
            onClick={() => item.route !== '/more' && history.push(item.route)}
          >
            <IonIcon icon={item.icon} />
            <span>{item.label}</span>
          </div>
        ))}
      </div>
    </IonPage>
  );
};

export default MorePage;
