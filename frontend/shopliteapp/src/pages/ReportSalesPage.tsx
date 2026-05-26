import React, { useEffect, useState } from 'react';
import {
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonToolbar,
} from '@ionic/react';
import {
  calendarOutline,
  caretDown,
  chevronBackOutline,
  chevronForwardOutline,
  downloadOutline,
  trendingUpOutline,
} from 'ionicons/icons';
import { useHistory } from 'react-router-dom';
import DateRangePickerModal, { DateRange, rangeLabelOf } from '../components/DateRangePickerModal';
import { reportService } from '../services/report.service';
import { getStoredStoreId } from '../utils/Apis';
import './Reports.css';

/* ---------- helpers ---------- */
const fmt = (v: number) =>
  new Intl.NumberFormat('vi-VN').format(Math.round(v)) + 'đ';

const fmtCount = (v: number) =>
  new Intl.NumberFormat('vi-VN').format(v);

const fmtK = (v: number) =>
  new Intl.NumberFormat('vi-VN').format(Math.round(v / 1_000)) + 'K';

/* ---------- types ---------- */
type Period = 'today' | 'week' | 'month' | 'quarter' | 'custom';

type RevenuePoint = { label: string; value: number };

interface SalesSummary {
  totalRevenue: number;
  totalOrders: number;
  totalDiscount: number;
  netRevenue: number;
  avgOrderValue: number;
  returnAmount: number;
  growth: number;
}

interface TopCategory {
  name: string;
  revenue: number;
  orders: number;
  pct: number;
}

interface RecentOrder {
  code: string;
  customer: string;
  amount: number;
  status: string;
  time: string;
}

const PERIODS: { key: Exclude<Period, 'custom'>; label: string }[] = [
  { key: 'today',   label: 'Hôm nay'   },
  { key: 'week',    label: '7 ngày'    },
  { key: 'month',   label: 'Tháng này' },
  { key: 'quarter', label: 'Quý này'   },
];

const STATUS_MAP: Record<string, { label: string; cls: string }> = {
  PAID:            { label: 'Đã thu',    cls: ''     },
  COMPLETED:       { label: 'Đã thu',    cls: ''     },
  PENDING:         { label: 'Chờ TT',   cls: 'blue' },
  PENDING_PAYMENT: { label: 'Chờ TT',   cls: 'blue' },
  DRAFT:           { label: 'Nháp',     cls: 'blue' },
  CANCELLED:       { label: 'Đã huỷ',  cls: 'red'  },
  FAIL:            { label: 'Thất bại', cls: 'red'  },
};
const getOrderStatus = (status: string) =>
  STATUS_MAP[status] ?? { label: status, cls: '' };

/* ============================================================ */
const ReportSalesPage: React.FC = () => {
  const history = useHistory();
  const [period, setPeriod]           = useState<Period>('today');
  const [customRange, setCustomRange] = useState<DateRange | null>(null);
  const [pickerOpen, setPickerOpen]   = useState(false);
  const [loading, setLoading]         = useState(false);
  const [summary, setSummary]         = useState<SalesSummary>({
    totalRevenue: 0, totalOrders: 0, totalDiscount: 0,
    netRevenue: 0, avgOrderValue: 0, returnAmount: 0, growth: 0,
  });
  const [categories, setCategories]   = useState<TopCategory[]>([]);
  const [recentOrders, setRecentOrders] = useState<RecentOrder[]>([]);
  const [revenuePoints, setRevenuePoints] = useState<RevenuePoint[]>([]);

  const periodToDateRange = (p: Period, cr: DateRange | null): { from: string; to: string } => {
    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];
    if (p === 'today') return { from: todayStr, to: todayStr };
    if (p === 'week') { const d = new Date(); d.setDate(d.getDate() - 6); return { from: d.toISOString().split('T')[0], to: todayStr }; }
    if (p === 'month') { const d = new Date(); d.setDate(d.getDate() - 29); return { from: d.toISOString().split('T')[0], to: todayStr }; }
    if (p === 'quarter') { const d = new Date(); d.setDate(d.getDate() - 89); return { from: d.toISOString().split('T')[0], to: todayStr }; }
    if (p === 'custom' && cr) return { from: cr.from, to: cr.to };
    return { from: todayStr, to: todayStr };
  };

  const load = async (p: Period = period, cr: DateRange | null = customRange) => {
    setLoading(true);
    const storeId = Number(getStoredStoreId() || 1);
    const { from, to } = periodToDateRange(p, cr);
    const res = await reportService.getSalesReport(storeId, p, from, to);
    if (res) {
      setSummary({
        totalRevenue: res.totalRevenue, totalOrders: res.totalOrders,
        totalDiscount: res.totalDiscount, netRevenue: res.netRevenue,
        avgOrderValue: res.avgOrderValue, returnAmount: res.returnAmount, growth: res.growth,
      });
      setCategories(res.topCategories || []);
      setRecentOrders((res.recentOrders || []).map((o: any) => ({
        code: o.code, customer: o.customer ?? 'Khách lẻ', amount: o.amount ?? 0,
        status: o.status ?? 'COMPLETED',
        time: o.time?.substring(11, 16) || o.time || '',
      })));
      setRevenuePoints(res.chartData || []);
    }
    setLoading(false);
  };

  useEffect(() => {
    load(period);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [period]);

  const handleCustomConfirm = (range: DateRange) => {
    setCustomRange(range);
    setPeriod('custom');
    setPickerOpen(false);
    load('custom', range);
  };

  const basePoints = revenuePoints.length > 0 ? revenuePoints : [];
  const maxPoint   = Math.max(...basePoints.map(p => p.value), 1);


  return (
    <IonPage className="rpt-page">
      <IonHeader className="rpt-header ion-no-border">
        <IonToolbar className="rpt-toolbar">
          <div className="rpt-toolbar-inner">
            <button className="rpt-back-btn" aria-label="Quay lại" onClick={() => history.goBack()}>
              <IonIcon icon={chevronBackOutline} />
            </button>
            <span className="rpt-title">Báo cáo bán hàng</span>
            <div className="rpt-header-actions">
              <button className="rpt-icon-btn" aria-label="Xuất báo cáo">
                <IonIcon icon={downloadOutline} />
              </button>
            </div>
          </div>
        </IonToolbar>

        <div className="rpt-period-bar">
          <button
            className={`rpt-period-custom ${period === 'custom' ? 'active' : ''}`}
            onClick={() => setPickerOpen(true)}
          >
            <IonIcon icon={calendarOutline} />
            {period === 'custom' && customRange
              ? rangeLabelOf(customRange.from, customRange.to)
              : 'Tùy chọn'}
            <IonIcon icon={caretDown} />
          </button>
          {PERIODS.map(p => (
            <button
              key={p.key}
              className={`rpt-period-chip ${period === p.key ? 'active' : ''}`}
              onClick={() => { setPeriod(p.key); setCustomRange(null); }}
            >
              {p.label}
            </button>
          ))}
        </div>
      </IonHeader>

      <IonContent className="rpt-content">
        <IonRefresher slot="fixed" onIonRefresh={async e => { load(); e.detail.complete(); }}>
          <IonRefresherContent />
        </IonRefresher>

        {loading ? (
          <div className="rpt-loading"><IonSpinner name="crescent" /></div>
        ) : (
          <div className="rpt-body">

            {/* Growth banner */}
            <div className="rpt-info-banner success">
              <IonIcon icon={trendingUpOutline} className="rpt-info-icon" />
              <div>
                <div className="rpt-info-title">
                  Tăng trưởng {summary.growth}% so với kỳ trước
                </div>
                <div className="rpt-info-sub">Doanh thu đang trên đà tăng tốt</div>
              </div>
            </div>

            {/* Summary cards */}
            <div className="rpt-summary-grid">
              <div className="rpt-summary-card accent-blue span-2">
                <div className="rpt-card-label">Doanh thu thuần</div>
                <div className="rpt-card-value">{fmt(summary.netRevenue)}</div>
                <div className="rpt-card-sub">
                  {fmtCount(summary.totalOrders)} đơn • Tổng thu: {fmt(summary.totalRevenue)}
                </div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Đơn hàng</div>
                <div className="rpt-card-value neutral">{fmtCount(summary.totalOrders)}</div>
                <div className="rpt-card-sub">đơn</div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">TB / đơn</div>
                <div className="rpt-card-value compact neutral">{fmt(summary.avgOrderValue)}</div>
                <div className="rpt-card-sub">giá trị trung bình</div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Giảm giá</div>
                <div className="rpt-card-value compact danger">{fmt(summary.totalDiscount)}</div>
                <div className="rpt-card-sub">chiết khấu</div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Hoàn trả</div>
                <div className="rpt-card-value compact warning">{fmt(summary.returnAmount)}</div>
                <div className="rpt-card-sub">trả lại KH</div>
              </div>
            </div>

            {/* Revenue chart */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Biểu đồ doanh thu</span>
              </div>
              <div className="rpt-chart-wrap">
                {basePoints.map(pt => (
                  <div key={pt.label} className="rpt-bar-row">
                    <span className="rpt-bar-label">{pt.label}</span>
                    <div className="rpt-bar-track">
                      <div
                        className="rpt-bar-fill green"
                        style={{ width: `${Math.round((pt.value / maxPoint) * 100)}%` }}
                      />
                    </div>
                    <span className="rpt-bar-val">{fmtK(pt.value)}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Top categories */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Danh mục bán chạy</span>
                <button className="rpt-section-link">
                  Xem tất cả <IonIcon icon={chevronForwardOutline} />
                </button>
              </div>
              {categories.map((cat, idx) => {
                const rankClass = idx === 0 ? 'gold' : idx === 1 ? 'silver' : idx === 2 ? 'bronze' : 'normal';
                return (
                  <div key={cat.name} className="rpt-progress-row">
                    <div className={`rpt-rank ${rankClass}`}>{idx + 1}</div>
                    <div className="rpt-progress-info">
                      <div className="rpt-progress-name">{cat.name}</div>
                      <div className="rpt-progress-track">
                        <div className="rpt-progress-fill green" style={{ width: `${cat.pct}%` }} />
                      </div>
                    </div>
                    <div className="rpt-progress-stat">
                      <div className="val">{fmt(cat.revenue)}</div>
                      <div className="sub">{fmtCount(cat.orders)} đơn</div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Recent orders */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Đơn hàng gần đây</span>
                <button className="rpt-section-link" onClick={() => history.push('/orders')}>
                  Xem tất cả <IonIcon icon={chevronForwardOutline} />
                </button>
              </div>
              {recentOrders.map(order => {
                const { label, cls } = getOrderStatus(order.status);
                return (
                  <div
                    key={order.code}
                    className="rpt-row"
                    onClick={() => history.push('/orders')}
                    style={{ cursor: 'pointer' }}
                  >
                    <div className="rpt-row-left">
                      <div className="rpt-row-title">{order.code}</div>
                      <div className="rpt-row-sub">{order.customer} • {order.time}</div>
                    </div>
                    <div className="rpt-row-right">
                      <div className="rpt-row-amount pos">{fmt(order.amount)}</div>
                      <div className={`rpt-row-badge ${cls}`}>{label}</div>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="rpt-bottom-spacer" />
          </div>
        )}
      </IonContent>

      <DateRangePickerModal
        isOpen={pickerOpen}
        initialRange={customRange ?? undefined}
        onConfirm={handleCustomConfirm}
        onClose={() => setPickerOpen(false)}
      />
    </IonPage>
  );
};

export default ReportSalesPage;
