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
    chevronForwardOutline,
    cubeOutline,
    ellipsisHorizontalOutline,
    informationCircleOutline,
} from 'ionicons/icons';
import type { Category, ProductUpsert, Unit } from '../api/types';
import { productService } from '../services/product.service';
import { ApiError, authApis, endpoints } from '../utils/Apis';
import { uploadToCloudinary } from '../utils/cloudinary';
import CategoryPickerModal from './CategoryPickerModal';
import UnitPickerModal from './UnitPickerModal';
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
    const [categoryName, setCategoryName] = useState<string>('');
    const [unitId, setUnitId] = useState<number | ''>('');
    const [unitName, setUnitName] = useState<string>('');
    const [sellingPrice, setSellingPrice] = useState<number | ''>('');
    const [costPrice, setCostPrice] = useState<number | ''>('');
    const [minStock, setMinStock] = useState<number | ''>('');
    const [maxStock, setMaxStock] = useState<number | ''>('');
    const [barcode, setBarcode] = useState('');
    const [imageUrl, setImageUrl] = useState<string | null>(null);
    const [uploading, setUploading] = useState(false);
    const fileInputRef = React.useRef<HTMLInputElement>(null);

    const [showCategoryPicker, setShowCategoryPicker] = useState(false);
    const [showUnitPicker, setShowUnitPicker] = useState(false);

    const [toast, setToast] = useState<string | null>(null);

    useIonViewWillEnter(() => {
        const load = async () => {
            setLoading(true);
            try {
                const [cats, unitRes] = await Promise.all([
                    productService.getCategories(),
                    authApis().get<any>(endpoints.units).catch(() => ({ data: { data: [] } })),
                ]);
                setCategories(cats);
                const rawUnits = (unitRes.data as { data?: Unit[] })?.data ?? unitRes.data;
                setUnits(Array.isArray(rawUnits) ? (rawUnits as Unit[]) : []);
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
                image: imageUrl,
            };

            await authApis().post<any>(endpoints.products, payload);
            setToast('Thêm thành công');
            setTimeout(() => {
                ionRouter.goBack();
            }, 1000);
        } catch (err: unknown) {
            setToast(err instanceof ApiError ? err.message : 'Thêm thất bại');
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
                        <div className="pa-title">Thêm hàng hóa</div>
                    </div>
                    <div slot="end">
                        <button className="pa-save-btn" onClick={handleSave} disabled={saving || loading}>
                            {saving ? <IonSpinner name="dots" /> : 'Lưu'}
                        </button>
                    </div>
                </IonToolbar>
            </IonHeader>

            <IonContent className="pa-content">
                <div className="pa-container">
                    <div className="pa-image-section">
                        <input
                            type="file"
                            accept="image/*"
                            ref={fileInputRef}
                            hidden
                            onChange={async (e) => {
                                const file = e.target.files?.[0];
                                if (!file) return;
                                setUploading(true);
                                try {
                                    const url = await uploadToCloudinary(file);
                                    if (url) {
                                        setImageUrl(url);
                                        setToast('Tải ảnh thành công');
                                    } else {
                                        setToast('Tải ảnh thất bại');
                                    }
                                } catch {
                                    setToast('Tải ảnh thất bại');
                                } finally {
                                    setUploading(false);
                                    e.target.value = '';
                                }
                            }}
                        />
                        <div className="pa-image-box" onClick={() => fileInputRef.current?.click()}>
                            {uploading ? (
                                <IonSpinner name="crescent" color="primary" />
                            ) : imageUrl ? (
                                <img src={imageUrl} alt="Product" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '12px' }} />
                            ) : (
                                <>
                                    <IonIcon icon={cameraOutline} />
                                    <span className="pa-image-text">THÊM ẢNH</span>
                                </>
                            )}
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
                            <button
                                type="button"
                                className="pa-picker-row"
                                onClick={() => setShowCategoryPicker(true)}
                            >
                                <span className={categoryName ? 'pa-picker-value' : 'pa-picker-placeholder'}>
                                    {categoryName || 'Chọn nhóm hàng'}
                                </span>
                                <IonIcon icon={chevronForwardOutline} className="pa-picker-chevron" />
                            </button>
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
                            <button
                                type="button"
                                className="pa-picker-row"
                                onClick={() => setShowUnitPicker(true)}
                            >
                                <span className={unitName ? 'pa-picker-value' : 'pa-picker-placeholder'}>
                                    {unitName || 'Chọn đơn vị tính'}
                                </span>
                                <IonIcon icon={chevronForwardOutline} className="pa-picker-chevron" />
                            </button>
                        </div>
                    </div>
                </div>
            </IonContent>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />

            <CategoryPickerModal
                isOpen={showCategoryPicker}
                selected={categoryId}
                onClose={() => setShowCategoryPicker(false)}
                onSelect={(cat) => {
                    setCategoryId(cat.id);
                    setCategoryName(cat.name);
                    setShowCategoryPicker(false);
                }}
            />

            <UnitPickerModal
                isOpen={showUnitPicker}
                selected={unitId}
                onClose={() => setShowUnitPicker(false)}
                onSelect={(unit) => {
                    setUnitId(unit.id);
                    setUnitName(unit.name);
                    setShowUnitPicker(false);
                }}
            />
        </IonPage>
    );
};

export default ProductAddPage;
