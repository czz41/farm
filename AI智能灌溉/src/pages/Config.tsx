import { useEffect, useState, useCallback, useRef } from "react";
import { motion } from "framer-motion";
import {
  Settings,
  Leaf,
  MapPin,
  Home,
  Trees,
  Bell,
  Zap,
  Hand,
  Sparkles,
  Save,
} from "lucide-react";
import PageHeader from "@/components/PageHeader";
import Switch from "@/components/Switch";
import Skeleton from "@/components/Skeleton";
import { useStore } from "@/store";
import { updateConfig, cityLookup } from "@/api/config";
import { generateAiPlan, publishPlan } from "@/api/plan";
import { PLAN_TYPE, PLANT_TYPE, SCENE, type SysConfig, type CityLookupVO } from "@/types";
import { cn } from "@/lib/utils";

export default function Config() {
  const { config, configLoading, loadConfig, pushToast } = useStore();
  const [form, setForm] = useState<Partial<SysConfig>>({});
  const [saving, setSaving] = useState(false);
  const [cityQuery, setCityQuery] = useState("");
  const [cityResults, setCityResults] = useState<CityLookupVO[]>([]);
  const [showCityDropdown, setShowCityDropdown] = useState(false);

  // 仅首次加载时同步 config → form，避免轮询刷新覆盖用户正在编辑的表单
  const syncedRef = useRef(false);
  useEffect(() => {
    loadConfig();
  }, [loadConfig]);
  useEffect(() => {
    if (config && !syncedRef.current) {
      setForm(config);
      syncedRef.current = true;
    }
  }, [config]);

  // 城市联想
  const searchCity = useCallback(async (q: string) => {
    setCityQuery(q);
    // 同步到 form.locationName，确保保存时城市名不会丢失
    setForm((f) => ({ ...f, locationName: q }));
    if (q.trim().length < 1) {
      setCityResults([]);
      return;
    }
    try {
      const list = await cityLookup(q.trim());
      setCityResults(list ?? []);
      setShowCityDropdown(true);
    } catch {
      setCityResults([]);
    }
  }, []);

  const set = <K extends keyof SysConfig>(key: K, val: SysConfig[K]) => {
    setForm((f) => ({ ...f, [key]: val }));
  };

  const handleSceneChange = (sceneType: number) => {
    // 室内场景关闭预警相关
    if (sceneType === SCENE.INDOOR) {
      setForm((f) => ({ ...f, sceneType, enableWarn: 0, enableAutoIntervene: 0 }));
    } else {
      set("sceneType", sceneType);
    }
  };

  const handleWarnChange = (on: boolean) => {
    setForm((f) => ({
      ...f,
      enableWarn: on ? 1 : 0,
      // 关闭预警时联动关闭自动介入
      enableAutoIntervene: on ? f.enableAutoIntervene : 0,
    }));
  };

  const handleInterveneChange = (on: boolean) => {
    // 仅在开启预警时允许
    if (on && !form.enableWarn) {
      pushToast("info", "请先开启预警通知");
      return;
    }
    set("enableAutoIntervene", on ? 1 : 0);
  };

  const handleSave = async () => {
    if (!form.mailAddr && form.enableWarn) {
      pushToast("error", "开启预警需填写邮箱地址");
      return;
    }
    setSaving(true);
    try {
      await updateConfig(form);
      // 保存成功后允许下次同步最新 config
      syncedRef.current = false;
      await loadConfig();
      // AI 模式：配置变更后重新生成 AI 方案并下发，确保方案与最新配置匹配
      if (form.currentPlanType === PLAN_TYPE.AI) {
        try {
          await generateAiPlan();
          await publishPlan();
        } catch {
          // 生成/下发失败不阻塞配置保存流程
        }
      }
      pushToast("success", "配置已保存");
    } catch (e) {
      pushToast("error", (e as Error).message || "保存失败");
    } finally {
      setSaving(false);
    }
  };

  const isOutdoor = form.sceneType === SCENE.OUTDOOR;

  if (configLoading && !config) {
    return (
      <div>
        <PageHeader title="基础配置" subtitle="植物信息 · 地域 · 场景 · 预警开关" icon={<Settings size={22} />} />
        <div className="glass rounded-3xl p-7"><Skeleton lines={8} /></div>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        title="基础配置"
        subtitle="植物信息 · 地域 · 场景 · 预警开关 · 模式选择"
        icon={<Settings size={22} />}
        actions={
          <button
            onClick={handleSave}
            disabled={saving}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-blue-500 to-blue-400 text-white font-body font-medium text-sm shadow-blue-500/25 hover:opacity-90 transition-opacity disabled:opacity-50"
          >
            <Save size={16} />
            {saving ? "保存中…" : "保存配置"}
          </button>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 植物信息 */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="glass rounded-3xl p-6">
          <div className="flex items-center gap-2 mb-5">
            <Leaf size={18} className="text-leaf" />
            <h3 className="font-display text-lg text-cream">植物信息</h3>
          </div>
          <div className="space-y-4">
            <Field label="植物名称">
              <input
                value={form.plantName ?? ""}
                onChange={(e) => set("plantName", e.target.value)}
                placeholder="如：绿萝、月季、番茄"
                className="input-field"
              />
            </Field>
            <Field label="特殊备注">
              <textarea
                value={form.specialNote ?? ""}
                onChange={(e) => set("specialNote", e.target.value)}
                placeholder="如：植株刚发芽，处于幼苗期"
                rows={3}
                className="input-field resize-none"
              />
            </Field>
            <Field label="种植方式">
              <div className="grid grid-cols-3 gap-3">
                <button
                  onClick={() => set("plantType", PLANT_TYPE.POT_SMALL)}
                  className={cn(
                    "flex flex-col items-center justify-center gap-1 py-3 rounded-xl border transition-all",
                    form.plantType === PLANT_TYPE.POT_SMALL
                      ? "bg-leaf/15 border-leaf/40 text-leaf"
                      : "border-cream/10 text-creamDim hover:border-cream/20"
                  )}
                >
                  <span className="text-sm">花盆盆栽</span>
                  <span className="text-xs opacity-70">20-25cm</span>
                </button>
                <button
                  onClick={() => set("plantType", PLANT_TYPE.POT_LARGE)}
                  className={cn(
                    "flex flex-col items-center justify-center gap-1 py-3 rounded-xl border transition-all",
                    form.plantType === PLANT_TYPE.POT_LARGE
                      ? "bg-leaf/15 border-leaf/40 text-leaf"
                      : "border-cream/10 text-creamDim hover:border-cream/20"
                  )}
                >
                  <span className="text-sm">大盆</span>
                  <span className="text-xs opacity-70">30cm以上</span>
                </button>
                <button
                  onClick={() => set("plantType", PLANT_TYPE.GROUND)}
                  className={cn(
                    "flex flex-col items-center justify-center gap-1 py-3 rounded-xl border transition-all",
                    form.plantType === PLANT_TYPE.GROUND
                      ? "bg-leaf/15 border-leaf/40 text-leaf"
                      : "border-cream/10 text-creamDim hover:border-cream/20"
                  )}
                >
                  <span className="text-sm">地栽单株</span>
                  <span className="text-xs opacity-70">菜园</span>
                </button>
              </div>
            </Field>
          </div>
        </motion.div>

        {/* 地域与场景 */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.05 }} className="glass rounded-3xl p-6">
          <div className="flex items-center gap-2 mb-5">
            <MapPin size={18} className="text-blue-400" />
            <h3 className="font-display text-lg text-cream">地域与场景</h3>
          </div>
          <div className="space-y-4">
            <Field label="所在城市">
              <div className="relative">
                <input
                  value={cityQuery || form.locationName || ""}
                  onChange={(e) => searchCity(e.target.value)}
                  onBlur={() => setTimeout(() => setShowCityDropdown(false), 200)}
                  onFocus={() => { if (cityResults.length > 0) setShowCityDropdown(true); }}
                  placeholder="如：北京、上海"
                  className="input-field"
                />
                {showCityDropdown && cityResults.length > 0 && (
                  <div className="absolute z-20 mt-1 w-full glass rounded-xl overflow-hidden max-h-48 overflow-y-auto">
                    {cityResults.map((c) => (
                      <button
                        key={c.id}
                        onClick={() => {
                          set("locationCode", c.id);
                          set("locationName", c.name);
                          setCityQuery(c.name);
                          setShowCityDropdown(false);
                        }}
                        className="w-full text-left px-4 py-2.5 hover:bg-blue-500/10 transition-colors flex items-center justify-between"
                      >
                        <span className="text-cream text-sm">{c.name}</span>
                        <span className="text-creamDim text-xs">{c.adm}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </Field>

            <Field label="种植场景">
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={() => handleSceneChange(SCENE.INDOOR)}
                  className={cn(
                    "flex items-center justify-center gap-2 py-3 rounded-xl border transition-all",
                    form.sceneType === SCENE.INDOOR
                      ? "bg-blue-500/15 border-blue-500/40 text-blue-400"
                      : "border-cream/10 text-creamDim hover:border-cream/20"
                  )}
                >
                  <Home size={16} /> 室内
                </button>
                <button
                  onClick={() => handleSceneChange(SCENE.OUTDOOR)}
                  className={cn(
                    "flex items-center justify-center gap-2 py-3 rounded-xl border transition-all",
                    form.sceneType === SCENE.OUTDOOR
                      ? "bg-water/15 border-water/40 text-water"
                      : "border-cream/10 text-creamDim hover:border-cream/20"
                  )}
                >
                  <Trees size={16} /> 室外
                </button>
              </div>
            </Field>
          </div>
        </motion.div>

        {/* 预警开关 */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="glass rounded-3xl p-6">
          <div className="flex items-center gap-2 mb-5">
            <Bell size={18} className="text-amber" />
            <h3 className="font-display text-lg text-cream">预警与介入</h3>
          </div>
          <div className={cn("space-y-4", !isOutdoor && "opacity-40 pointer-events-none")}>
            <div className="flex items-center justify-between p-4 rounded-xl bg-cream/5 border border-cream/5">
              <div className="flex items-center gap-3">
                <Bell size={16} className="text-amber" />
                <div>
                  <div className="text-cream text-sm font-body">预警通知</div>
                  <div className="text-creamDim text-xs">检测极端天气并推送邮件</div>
                </div>
              </div>
              <Switch checked={!!form.enableWarn} onChange={handleWarnChange} disabled={!isOutdoor} />
            </div>
            <div className="flex items-center justify-between p-4 rounded-xl bg-cream/5 border border-cream/5">
              <div className="flex items-center gap-3">
                <Zap size={16} className="text-rust" />
                <div>
                  <div className="text-cream text-sm font-body">自动介入</div>
                  <div className="text-creamDim text-xs">生成临时方案，AI模式自动下发</div>
                </div>
              </div>
              <Switch
                checked={!!form.enableAutoIntervene}
                onChange={handleInterveneChange}
                disabled={!isOutdoor || !form.enableWarn}
              />
            </div>
            {!isOutdoor && (
              <p className="text-creamDim text-xs">室内场景无气象预警逻辑，AI方案会自动下调水量</p>
            )}
          </div>
        </motion.div>

        {/* 模式与邮箱 */}
        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.15 }} className="glass rounded-3xl p-6">
          <div className="flex items-center gap-2 mb-5">
            <Sparkles size={18} className="text-leaf" />
            <h3 className="font-display text-lg text-cream">模式与通知</h3>
          </div>
          <div className="space-y-4">
            <Field label="当前执行模式">
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={() => set("currentPlanType", PLAN_TYPE.MANUAL)}
                  className={cn(
                    "flex items-center justify-center gap-2 py-3 rounded-xl border transition-all",
                    form.currentPlanType === PLAN_TYPE.MANUAL
                      ? "bg-leaf/15 border-leaf/40 text-leaf"
                      : "border-cream/10 text-creamDim hover:border-cream/20"
                  )}
                >
                  <Hand size={16} /> 人工模式
                </button>
                <button
                  onClick={() => set("currentPlanType", PLAN_TYPE.AI)}
                  className={cn(
                    "flex items-center justify-center gap-2 py-3 rounded-xl border transition-all",
                    form.currentPlanType === PLAN_TYPE.AI
                      ? "bg-blue-400/15 border-blue-400/40 text-blue-400"
                      : "border-cream/10 text-creamDim hover:border-cream/20"
                  )}
                >
                  <Sparkles size={16} /> AI模式
                </button>
              </div>
              {form.currentPlanType === PLAN_TYPE.TEMP && (
                <p className="text-amber text-xs mt-2">当前处于临时方案模式，极端天气恢复后自动切回</p>
              )}
            </Field>
            <Field label="预警邮箱">
              <input
                value={form.mailAddr ?? ""}
                onChange={(e) => set("mailAddr", e.target.value)}
                placeholder="接收预警邮件的邮箱"
                className="input-field"
              />
            </Field>
          </div>
        </motion.div>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-creamDim text-xs font-body mb-2">{label}</label>
      {children}
    </div>
  );
}
