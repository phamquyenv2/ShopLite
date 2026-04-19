import React, { useState } from 'react';
import {
    IonContent,
    IonHeader,
    IonIcon,
    IonPage,
    IonSpinner,
    IonToast,
    IonToolbar,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import {
    arrowBackOutline,
    barcodeOutline,
    cameraOutline,
    cubeOutline,
    ellipsisHorizontalOutline,
    informationCircleOutline,
} from 'ionicons/icons';
import type { Category, ProductUpsert, Unit } from '../api/types';
import { productService } from '../services/product.service';
import { ApiError, authApis, endpoints } from '../utils/Apis';
import './ProductAddPage.css';

const ProductAddPage: React.FC = () => {
    const ionRouter = useIonRouter();

    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [categories, setCategories] = useState<Category[]>([]);
    const [units, setUnits] = useState<Unit[]>([]);

    const [name, setName] = useState('');
    const [sku, setSku] = useState('');
    const [categoryId, setCategoryId] = useState<number | ''>('');
    const [unitId, setUnitId] = useState<number | ''>('');
    const [sellingPrice, setSellingPrice] = useState<number | ''>('');
    const [costPrice, setCostPrice] = useState<number | ''>('');
    const [minStock, setMinStock] = useState<number | ''>('');
    const [maxStock, setMaxStock] = useState<number | ''>('');
    const [barcode, setBarcode] = useState('');

    const [toast, setToast] = useState<string | null>(null);

    useIonViewWillEnter(() => {
        const load = async () => {
            setLoading(true);
            try {
                const [cats, unitRes] = await Promise.all([
                    productService.getCategories(),
                    authApis().get(endpoints.units).catch(() => ({ data: { data: [] } })),
                ]);
                setCategories(cats);
                setUnits(unitRes.data?.data || []);
            } catch (err) {
                setToast(err instanceof ApiError ? err.message : 'Khong the tai du lieu form');
            } finally {
                setLoading(false);
            }
        };
        void load();
    });

    const handleSave = async () => {
        if (!name.trim()) {
            setToast('Vui long nhap ten hang');
            return;
        }
        if (categoryId === '') {
            setToast('Vui long chon nhom hang');
            return;
        }
        if (unitId === '') {
            setToast('Vui long chon don vi tinh');
            return;
        }

        setSaving(true);
        try {
            const payload: ProductUpsert = {
                name: name.trim(),
                sku: sku.trim() || undefined,
                categoryId,
                unitId,
                sellingPrice: sellingPrice === '' ? 0 : sellingPrice,
                costPrice: costPrice === '' ? 0 : costPrice,
                minStock: minStock === '' ? null : minStock,
                maxStock: maxStock === '' ? null : maxStock,
                barcode: barcode.trim() || undefined,
                stock: 0,
            };

            await authApis().post(endpoints.products, payload);
            setToast('Them thanh cong');
            setTimeout(() => {
                ionRouter.goBack();
            }, 1000);
        } catch (err: unknown) {
            setToast(err instanceof ApiError ? err.message : 'Them that bai');
        } finally {
            setSaving(false);
        }
    };

    return (
        <IonPage className="product-add-page">
            <IonHeader className="ion-no-border pa-header">
                <IonToolbar className="pa-toolbar">
                    <div className="pa-toolbar-left" slot="start">
                        <button className="pa-icon-btn" type="button" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={arrowBackOutline} />
                        </button>
                        <div className="pa-title">Them hang hoa</div>
                    </div>
                    <div slot="end">
                        <button className="pa-save-btn" onClick={handleSave} disabled={saving || loading}>
                            {saving ? <IonSpinner name="dots" /> : 'Luu'}
                        </button>
                    </div>
                </IonToolbar>
            </IonHeader>

            <IonContent className="pa-content">
                <div className="pa-container">
                    <div className="pa-image-section">
                        <div className="pa-image-box" onClick={() => setToast('Tính năng tải ảnh đang phát triển')}>
                            <IonIcon icon={cameraOutline} />
                            <span className="pa-image-text">THÊM ẢNH</span>
                        </div>
                    </div>

                    <div className="pa-card">
                        <div className="pa-card-header">
                            <div className="pa-card-icon blue-bg"><IonIcon icon={informationCircleOutline} /></div>
                            THÔNG TIN CHUNG
                        </div>

                        <div className="pa-field">
                            <label className="pa-label">TÊN HÀNG <span>*</span></label>
                            <div className="pa-input-wrap">
                                <input className="pa-input" type="text" placeholder="Nhập tên sản phẩm..." value={name} onChange={(e) => setName(e.target.value)} />
                            </div>
                        </div>

                        <div className="pa-row">
                            <div className="pa-field">
                                <label className="pa-label">MÃ HÀNG</label>
                                <div className="pa-input-wrap">
                                    <input className="pa-input" type="text" disabled placeholder="Mã tự động" />
                                </div>
                            </div>
                            <div className="pa-field">
                                <label className="pa-label">SKU</label>
                                <div className="pa-input-wrap">
                                    <input className="pa-input" type="text" placeholder="SKU hàng hóa" value={sku} onChange={(e) => setSku(e.target.value)} />
                                </div>
                            </div>
                        </div>

                        <div className="pa-field">
                            <label className="pa-label">NHÓM HÀNG</label>
                            <select className="pa-select-box" value={categoryId} onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : '')}>
                                <option value="">Chọn nhóm hàng</option>
                                {categories.map((c) => (
                                    <option key={c.id} value={c.id}>{c.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="pa-card">
                        <div className="pa-card-header">
                            <div className="pa-card-icon blue-bg"><IonIcon icon={cubeOutline} /></div>
                            GIÁ & TỒN KHO
                        </div>

                        <div className="pa-row">
                            <div className="pa-field">
                                <label className="pa-label">GIÁ BÁN</label>
                                <div className="pa-input-wrap">
                                    <input className="pa-input has-suffix" type="number" placeholder="0" value={sellingPrice} onChange={(e) => setSellingPrice(e.target.value ? Number(e.target.value) : '')} />
                                    <span className="pa-suffix blue">đ</span>
                                </div>
                            </div>
                            <div className="pa-field">
                                <label className="pa-label">GIÁ VỐN</label>
                                <div className="pa-input-wrap">
                                    <input className="pa-input has-suffix" type="number" placeholder="0" value={costPrice} onChange={(e) => setCostPrice(e.target.value ? Number(e.target.value) : '')} />
                                    <span className="pa-suffix">đ</span>
                                </div>
                            </div>
                        </div>

                        <div className="pa-row">
                            <div className="pa-field">
                                <label className="pa-label">TỒN TỐI THIỂU</label>
                                <div className="pa-input-wrap">
                                    <input className="pa-input" type="number" placeholder="0" value={minStock} onChange={(e) => setMinStock(e.target.value ? Number(e.target.value) : '')} />
                                </div>
                            </div>
                            <div className="pa-field">
                                <label className="pa-label">TỒN TỐI ĐA</label>
                                <div className="pa-input-wrap">
                                    <input className="pa-input" type="number" placeholder="0" value={maxStock} onChange={(e) => setMaxStock(e.target.value ? Number(e.target.value) : '')} />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="pa-card">
                        <div className="pa-card-header">
                            <div className="pa-card-icon dots"><IonIcon icon={ellipsisHorizontalOutline} /></div>
                            THÔNG TIN BỔ SUNG
                        </div>

                        <div className="pa-field">
                            <label className="pa-label">MÃ VẠCH (BARCODE)</label>
                            <div className="pa-input-wrap">
                                <input className="pa-input" type="text" placeholder="Quét hoặc nhập mã vạch" value={barcode} onChange={(e) => setBarcode(e.target.value)} />
                                <button className="pa-scan-btn" type="button"><IonIcon icon={barcodeOutline} /></button>
                            </div>
                        </div>

                        <div className="pa-field" style={{ paddingBottom: '32px' }}>
                            <label className="pa-label">ĐƠN VỊ TÍNH</label>
                            <select className="pa-select-box" value={unitId} onChange={(e) => setUnitId(e.target.value ? Number(e.target.value) : '')}>
                                <option value="">Chọn đơn vị tính</option>
                                {units.map((u) => (
                                    <option key={u.id} value={u.id}>{u.name}</option>
                                ))}
                            </select>
                        </div>
                    </div>
                </div>
            </IonContent>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default ProductAddPage;
