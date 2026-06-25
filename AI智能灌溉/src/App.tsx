import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { useEffect } from "react";
import Layout from "@/components/Layout";
import Toast from "@/components/Toast";
import { useStore } from "@/store";
import { usePolling } from "@/hooks/usePolling";
import Overview from "@/pages/Overview";
import Config from "@/pages/Config";
import ManualPlan from "@/pages/ManualPlan";
import AiPlan from "@/pages/AiPlan";
import TempPlan from "@/pages/TempPlan";
import WarnHistory from "@/pages/WarnHistory";
import TestPanel from "@/pages/TestPanel";

export default function App() {
  const { loadConfig, loadDevice } = useStore();

  useEffect(() => {
    loadConfig();
    loadDevice();
  }, [loadConfig, loadDevice]);

  // 每 60 秒刷新配置（捕捉 current_plan_type 变化）
  usePolling(loadConfig, 60000);
  // 每 30 秒刷新设备状态
  usePolling(loadDevice, 30000);

  return (
    <Router>
      <Toast />
      <Layout>
        <Routes>
          <Route path="/" element={<Overview />} />
          <Route path="/config" element={<Config />} />
          <Route path="/manual" element={<ManualPlan />} />
          <Route path="/ai" element={<AiPlan />} />
          <Route path="/temp" element={<TempPlan />} />
          <Route path="/warn" element={<WarnHistory />} />
          <Route path="/test" element={<TestPanel />} />
        </Routes>
      </Layout>
    </Router>
  );
}
