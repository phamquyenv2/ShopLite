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
  alertCircleOutline,
  archiveOutline,
  calendarOutline,
  caretDown,
  chevronBackOutline,
  chevronForwardOutline,
  downloadOutline,
  trendingDownOutline,
} from 'ionicons/icons';
import { useHistory } from 'react-router-dom';
import DateRangePickerModal, { DateRange, rangeLabelOf } from '../components/DateRangePickerModal';
import './Reports.css';

/* ---------- helpers ---------- */
const fmt = (v: number) =>
  new Intl.NumberFormat('vi-VN').format(Math.round(v)) + 'đ';

const fmtCount = (v: number) =>
  new Intl.NumberFormat('vi-VN').format(v);

/* ---------- types ---------- */
type Period = 'today' | 'week' | 'month' | 'custom';

interface InventorySummary {
  totalSku: number;
  totalStock: number;
  totalValue: number;
  lowStockCount: number;
  outOfStockCount: number;
  newImportValue: number;
  soldUnits: number;
}

interface LowStockItem {
  name: string;
  sku: string;
  stock: number;
  minStock: number;
}

interface MovementItem {
  name: string;
  sold: number;
  imported: number;
  adjusted: number;
  currentStock: number;
}

/* ---------- mock data ---------- */
const mockSummary = (): InventorySummary => ({
  totalSku:        148,
  totalStock:      2_340,
  totalValue:      284_500_000,
  lowStockCount:   12,
  outOfStockCount: 3,
  newImportValue:  45_200_000,
  soldUnits:       167,
});

const mockLowStock = (): LowStockItem[] => [
  { name: 'Cà phê Arabica 500g',   sku: 'CF-001',  stock: 2,  minStock: 10  },
  { name: 'Trà Ô Long túi lọc',    sku: 'TRA-008', stock: 3,  minStock: 15  },
  { name: 'Đường cát trắng 1kg',   sku: 'DUG-003', stock: 1,  minStock: 20  },
  { name: 'Nước khoáng 500ml',     sku: 'NUO-012', stock: 5,  minStock: 50  },
  { name: 'Cốc dùng 1 lần',        sku: 'COC-002', stock: 8,  minStock: 100 },
];

const mockMovements = (): MovementItem[] => [
  { name: 'Cà phê sữa đá',  sold: 42, imported:  0, adjusted:  0, currentStock: 86 },
  { name: 'Bánh mì thịt',   sold: 35, imported: 50, adjusted:  0, currentStock: 63 },
  { name: 'Nước cam ép',    sold: 28, imported: 30, adjusted: -2, currentStock: 44 },
  { name: 'Trà đào cam sả', sold: 22, imported:  0, adjusted:  0, currentStock: 18 },
  { name: 'Sandwich trứng', sold: 18, imported: 20, adjusted:  0, currentStock: 32 },
];

const PERIODS: { key: Exclude<Period, 'custom'>; label: string }[] = [
  { key: 'today', label: 'Hôm nay'   },
  { key: 'week',  label: '7 ngày'    },
  { key: 'month', label: 'Tháng này' },
];

/* ============================================================ */
const ReportInventoryPage: React.FC = () => {
  const history = useHistory();
  const [period, setPeriod]           = useState<Period>('today');
  const [customRange, setCustomRange] = useState<DateRange | null>(null);
  const [pickerOpen, setPickerOpen]   = useState(false);
  const [loading, setLoading]         = useState(false);
  const [summary, setSummary]         = useState<InventorySummary>(mockSummary());
  const [lowStockItems, setLowStockItems] = useState<LowStockItem[]>(mockLowStock());
  const [movements, setMovements]     = useState<MovementItem[]>(mockMovements());
  const [activeFilter, setActiveFilter] = useState<'all' | 'low' | 'out'>('all');

  const load = () => {
    setLoading(true);
    setTimeout(() => {
      setSummary(mockSummary());
      setLowStockItems(mockLowStock());
      setMovements(mockMovements());
      setLoading(false);
    }, 500);
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { load(); }, [period]);

  const handleCustomConfirm = (range: DateRange) => {
    setCustomRange(range);
    setPeriod('custom');
    setPickerOpen(false);
    load();
  };

  const stockHealth =
    summary.totalSku > 0
      ? Math.round(
          ((summary.totalSku - summary.lowStockCount - summary.outOfStockCount) / summary.totalSku) * 100
        )
      : 100;

  const healthCls = stockHealth >= 80 ? 'good' : stockHealth >= 50 ? 'warn' : 'bad';

  const hasAlert = summary.lowStockCount > 0 || summary.outOfStockCount > 0;

  return (
    <IonPage className="rpt-page">
      <IonHeader className="rpt-header ion-no-border">
        <IonToolbar className="rpt-toolbar">
          <div className="rpt-toolbar-inner">
            <button className="rpt-back-btn" aria-label="Quay lại" onClick={() => history.goBack()}>
              <IonIcon icon={chevronBackOutline} />
            </button>
            <span className="rpt-title">Báo cáo hàng hóa</span>
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

            {/* Alert banner */}
            {hasAlert && (
              <div className="rpt-info-banner warning">
                <IonIcon icon={alertCircleOutline} className="rpt-info-icon" />
                <div>
                  <div className="rpt-info-title">
                    {summary.outOfStockCount > 0
                      ? `${summary.outOfStockCount} mặt hàng hết hàng`
                      : `${summary.lowStockCount} mặt hàng sắp hết`}
                  </div>
                  <div className="rpt-info-sub">Cần nhập thêm hàng sớm</div>
                </div>
              </div>
            )}

            {/* Summary cards */}
            <div className="rpt-summary-grid">
              <div className="rpt-summary-card accent-blue span-2">
                <div className="rpt-card-label">Tổng giá trị tồn kho</div>
                <div className="rpt-card-value">{fmt(summary.totalValue)}</div>
                <div className="rpt-card-sub">
                  {fmtCount(summary.totalSku)} SKU • {fmtCount(summary.totalStock)} đơn vị
                </div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Đã bán ra</div>
                <div className="rpt-card-value success">{fmtCount(summary.soldUnits)}</div>
                <div className="rpt-card-sub">đơn vị</div>
              </div>

              <div className="rpt-summary-card">
                <div className="rpt-card-label">Nhập thêm</div>
                <div className="rpt-card-value compact neutral">{fmt(summary.newImportValue)}</div>
                <div className="rpt-card-sub">giá trị nhập</div>
              </div>

              <div className={`rpt-summary-card rpt-highlight-card ${summary.lowStockCount > 0 ? 'warning' : ''}`}>
                <div className="rpt-card-label">⚠️ Sắp hết hàng</div>
                <div className="rpt-card-value compact warning">{fmtCount(summary.lowStockCount)}</div>
                <div className="rpt-card-sub">mặt hàng</div>
              </div>

              <div className={`rpt-summary-card rpt-highlight-card ${summary.outOfStockCount > 0 ? 'danger' : ''}`}>
                <div className="rpt-card-label">🚫 Hết hàng</div>
                <div className="rpt-card-value compact danger">{fmtCount(summary.outOfStockCount)}</div>
                <div className="rpt-card-sub">mặt hàng</div>
              </div>
            </div>

            {/* Stock health gauge */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Sức khoẻ tồn kho</span>
                <span className={`rpt-health-score ${healthCls}`}>{stockHealth}%</span>
              </div>
              <div className="rpt-stock-health-body">
                <div className="rpt-stock-health-track">
                  <div
                    className={`rpt-stock-health-fill ${healthCls}`}
                    style={{ width: `${stockHealth}%` }}
                  />
                </div>
                <div className="rpt-stock-health-legend">
                  <span>🟢 Bình thường: {summary.totalSku - summary.lowStockCount - summary.outOfStockCount} SKU</span>
                  <span>🟡 Sắp hết: {summary.lowStockCount} &nbsp;•&nbsp; 🔴 Hết: {summary.outOfStockCount}</span>
                </div>
              </div>
            </div>

            {/* Low stock list */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Cần nhập thêm</span>
                <button className="rpt-section-link" onClick={() => history.push('/import-orders')}>
                  Nhập hàng <IonIcon icon={chevronForwardOutline} />
                </button>
              </div>
              {lowStockItems.map(item => {
                const isOut = item.stock === 0;
                return (
                  <div key={item.sku} className="rpt-row">
                    <div className={`rpt-dot ${isOut ? 'red' : 'orange'}`} />
                    <div className="rpt-row-left">
                      <div className="rpt-row-title">{item.name}</div>
                      <div className="rpt-row-sub">{item.sku} • Tối thiểu: {fmtCount(item.minStock)}</div>
                    </div>
                    <div className="rpt-row-right">
                      <div className={`rpt-row-amount ${isOut ? 'neg' : 'warning'}`}>
                        {fmtCount(item.stock)}
                      </div>
                      <div className={`rpt-row-badge ${isOut ? 'red' : ''}`}>
                        {isOut ? 'Hết hàng' : 'Sắp hết'}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Movements */}
            <div className="rpt-section">
              <div className="rpt-section-header">
                <span className="rpt-section-title">Biến động hàng hóa</span>
                <button className="rpt-section-link" onClick={() => history.push('/inventory-adjustments')}>
                  Kiểm kho <IonIcon icon={chevronForwardOutline} />
                </button>
              </div>

              <div className="rpt-filter-row">
                {([
                  { key: 'all', label: 'Tất cả'    },
                  { key: 'low', label: '⚠️ Sắp hết' },
                  { key: 'out', label: '🚫 Hết hàng' },
                ] as const).map(f => (
                  <button
                    key={f.key}
                    className={`rpt-filter-chip ${activeFilter === f.key ? 'active' : ''}`}
                    onClick={() => setActiveFilter(f.key)}
                  >
                    {f.label}
                  </button>
                ))}
              </div>

              {movements.map(m => (
                <div key={m.name} className="rpt-row">
                  <div className="rpt-movement-left">
                    <div className="rpt-movement-icon">
                      <IonIcon icon={archiveOutline} />
                    </div>
                    <div className="rpt-row-left">
                      <div className="rpt-row-title">{m.name}</div>
                      <div className="rpt-row-sub">
                        Bán: -{m.sold}
                        {m.imported > 0  ? ` • Nhập: +${m.imported}` : ''}
                        {m.adjusted !== 0 ? ` • ĐC: ${m.adjusted > 0 ? '+' : ''}${m.adjusted}` : ''}
                      </div>
                    </div>
                  </div>
                  <div className="rpt-row-right">
                    <div className="rpt-row-amount">{fmtCount(m.currentStock)}</div>
                    <div className="rpt-row-sub align-right">còn lại</div>
                  </div>
                </div>
              ))}
            </div>

            {/* Quick actions */}
            <div className="rpt-quick-actions">
              <button
                className="rpt-quick-btn primary"
                onClick={() => history.push('/import-order/new')}
              >
                <IonIcon icon={chevronForwardOutline} />
                Tạo đơn nhập hàng
              </button>
              <button
                className="rpt-quick-btn secondary"
                onClick={() => history.push('/inventory-adjustment/new')}
              >
                <IonIcon icon={trendingDownOutline} />
                Kiểm kho
              </button>
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

export default ReportInventoryPage;
