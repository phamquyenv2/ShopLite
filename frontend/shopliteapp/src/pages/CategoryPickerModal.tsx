import React, { useEffect, useRef, useState } from 'react';
import { IonIcon, IonSpinner, IonToast } from '@ionic/react';
import {
    addOutline,
    arrowBackOutline,
    chevronBackOutline,
    chevronForwardOutline,
    searchOutline,
} from 'ionicons/icons';
import type { Category } from '../api/types';
import { authApis, endpoints } from '../utils/Apis';
import { productService } from '../services/product.service';
import './CategoryPickerModal.css';

interface CategoryPickerModalProps {
    isOpen: boolean;
    selected: number | '';
    onClose: () => void;
    onSelect: (cat: Category) => void;
}

type SubView = 'list' | 'add-new';

const CategoryPickerModal: React.FC<CategoryPickerModalProps> = ({
    isOpen,
    selected,
    onClose,
    onSelect,
}) => {
    const [view, setView] = useState<SubView>('list');
    const [categories, setCategories] = useState<Category[]>([]);
    const [loadingList, setLoadingList] = useState(false);
    const [keyword, setKeyword] = useState('');

    // Add-new form
    const [newName, setNewName] = useState('');
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const searchRef = useRef<HTMLInputElement>(null);

    const reload = async () => {
        setLoadingList(true);
        try {
            const cats = await productService.getCategories();
            setCategories(cats);
        } finally {
            setLoadingList(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            setView('list');
            setKeyword('');
            setNewName('');
            void reload();
        }
    }, [isOpen]);

    const filtered = categories.filter((c) =>
        c.name.toLowerCase().includes(keyword.toLowerCase())
    );

    const handleAddNew = async () => {
        if (!newName.trim()) {
            setToast('Vui lòng nhập tên nhóm');
            return;
        }
        setSaving(true);
        try {
            const res = await authApis().post(endpoints.categories, { name: newName.trim() });
            const raw = (res.data as { id?: number; data?: { id?: number; name?: string }; name?: string }) ?? {};
            const savedId: number =
                typeof (raw as { id?: number }).id === 'number'
                    ? (raw as { id: number }).id
                    : typeof (raw as { data?: { id?: number } }).data?.id === 'number'
                        ? (raw as { data: { id: number } }).data.id
                        : 0;
            const savedName: string =
                typeof (raw as { name?: string }).name === 'string'
                    ? (raw as { name: string }).name
                    : typeof (raw as { data?: { name?: string } }).data?.name === 'string'
                        ? (raw as { data: { name: string } }).data.name
                        : newName.trim();

            setToast('Thêm nhóm thành công');
            setNewName('');
            await reload();
            if (savedId) {
                onSelect({ id: savedId, name: savedName });
            } else {
                setView('list');
            }
        } catch {
            setToast('Thêm nhóm thất bại');
        } finally {
            setSaving(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="cpm-overlay">
            <div className={`cpm-sheet ${isOpen ? 'cpm-sheet--in' : ''}`}>
                {/* ───── LIST VIEW ───── */}
                {view === 'list' && (
                    <>
                        <div className="cpm-header">
                            <button className="cpm-back-btn" onClick={onClose}>
                                <IonIcon icon={chevronBackOutline} />
                            </button>
                            <span className="cpm-title">Chọn nhóm hàng</span>
                            <button
                                className="cpm-add-btn"
                                onClick={() => { setNewName(''); setView('add-new'); }}
                            >
                                <IonIcon icon={addOutline} />
                            </button>
                        </div>

                        <div className="cpm-search-bar">
                            <IonIcon icon={searchOutline} className="cpm-search-icon" />
                            <input
                                ref={searchRef}
                                className="cpm-search-input"
                                placeholder="Tìm kiếm"
                                value={keyword}
                                onChange={(e) => setKeyword(e.target.value)}
                            />
                        </div>

                        <div className="cpm-list-wrap">
                            {loadingList ? (
                                <div className="cpm-center">
                                    <IonSpinner name="crescent" color="primary" />
                                </div>
                            ) : filtered.length === 0 ? (
                                <div className="cpm-empty">Không tìm thấy nhóm hàng</div>
                            ) : (
                                <div className="cpm-card">
                                    {filtered.map((cat, idx) => (
                                        <button
                                            key={cat.id}
                                            className={`cpm-item ${selected === cat.id ? 'cpm-item--active' : ''} ${idx < filtered.length - 1 ? 'cpm-item--divider' : ''}`}
                                            onClick={() => onSelect(cat)}
                                        >
                                            <span>{cat.name}</span>
                                            {selected === cat.id && (
                                                <span className="cpm-item-check">✓</span>
                                            )}
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>
                    </>
                )}

                {/* ───── ADD NEW VIEW ───── */}
                {view === 'add-new' && (
                    <>
                        <div className="cpm-header">
                            <button className="cpm-back-btn" onClick={() => setView('list')}>
                                <IonIcon icon={chevronBackOutline} />
                            </button>
                            <span className="cpm-title">Nhóm hàng mới</span>
                            <button
                                className="cpm-save-text-btn"
                                onClick={handleAddNew}
                                disabled={saving}
                            >
                                {saving ? <IonSpinner name="dots" /> : 'Lưu'}
                            </button>
                        </div>

                        <div className="cpm-form-card">
                            <div className="cpm-form-field">
                                <input
                                    className="cpm-form-input"
                                    placeholder="Tên nhóm *"
                                    value={newName}
                                    onChange={(e) => setNewName(e.target.value)}
                                    autoFocus
                                />
                            </div>
                            <div className="cpm-form-field cpm-form-field--row cpm-form-field--last">
                                <input
                                    className="cpm-form-input"
                                    placeholder="Nhóm cha"
                                    disabled
                                    style={{ flex: 1 }}
                                />
                                <IonIcon icon={chevronForwardOutline} className="cpm-chevron" />
                            </div>
                        </div>
                    </>
                )}
            </div>

            <IonToast
                isOpen={toast !== null}
                message={toast ?? ''}
                duration={2000}
                onDidDismiss={() => setToast(null)}
            />
        </div>
    );
};

export default CategoryPickerModal;
