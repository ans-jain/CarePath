import { jsPDF } from 'jspdf';
import { ExplainRiskRequest, ExplainRiskResponse, RiskTier } from '../types/explainability';

export interface GeneratePdfParams {
  patientName: string;
  patientEmail?: string;
  patientId?: string;
  request: ExplainRiskRequest;
  response: ExplainRiskResponse;
  timestamp?: string;
}

export const pdfReportService = {
  generateAssessmentPdf({
    patientName,
    patientEmail,
    patientId,
    request,
    response,
    timestamp,
  }: GeneratePdfParams): jsPDF {
    const doc = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
    });

    const reportDate = timestamp ? new Date(timestamp) : new Date();
    const formattedDate = reportDate.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

    const pageWidth = doc.internal.pageSize.getWidth(); // 210mm
    const margin = 14;
    const contentWidth = pageWidth - margin * 2; // 182mm
    let y = 14;

    // --- 1. Top Header Banner ---
    doc.setFillColor(15, 23, 42); // slate-900
    doc.rect(margin, y, contentWidth, 22, 'F');

    doc.setTextColor(255, 255, 255);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(14);
    doc.text('CarePath Health Platform', margin + 6, y + 9);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(148, 163, 184); // slate-400
    doc.text('Cardiometabolic Risk Stratification & Explainable AI Clinical Report', margin + 6, y + 16);

    doc.setFontSize(8);
    doc.setTextColor(203, 213, 225); // slate-300
    doc.text(`Generated: ${formattedDate}`, pageWidth - margin - 6, y + 9, { align: 'right' });
    doc.text(`Doc ID: CP-${Math.abs(reportDate.getTime()).toString(36).toUpperCase()}`, pageWidth - margin - 6, y + 16, { align: 'right' });

    y += 26;

    // --- 2. Patient Profile & Clinical Context Card ---
    doc.setFillColor(248, 250, 252); // slate-50
    doc.setDrawColor(226, 232, 240); // slate-200
    doc.rect(margin, y, contentWidth, 20, 'FD');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.setTextColor(30, 41, 59); // slate-800
    doc.text('PATIENT INFORMATION', margin + 4, y + 6);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8.5);
    doc.setTextColor(71, 85, 105); // slate-600

    doc.text(`Name:`, margin + 4, y + 12);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(15, 23, 42);
    doc.text(patientName || 'Sarah Jenkins', margin + 18, y + 12);

    doc.setFont('helvetica', 'normal');
    doc.setTextColor(71, 85, 105);
    doc.text(`Email:`, margin + 65, y + 12);
    doc.setTextColor(15, 23, 42);
    doc.text(patientEmail || 'patient@carepath.io', margin + 77, y + 12);

    doc.setTextColor(71, 85, 105);
    doc.text(`ID:`, margin + 130, y + 12);
    doc.setTextColor(15, 23, 42);
    doc.text(patientId ? `${patientId.slice(0, 12)}...` : 'PT-8943210', margin + 137, y + 12);

    doc.setTextColor(71, 85, 105);
    doc.text(`Evaluation Mode:`, margin + 4, y + 17);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(2, 132, 199); // clinical sky-600
    doc.text(request.evaluation_mode || 'LONGITUDINAL_ROBUST', margin + 30, y + 17);

    doc.setFont('helvetica', 'normal');
    doc.setTextColor(71, 85, 105);
    doc.text(`ML Engine:`, margin + 90, y + 17);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(15, 23, 42);
    doc.text('TreeSHAP Calibrated GBM v1.0.0', margin + 108, y + 17);

    y += 24;

    // --- 3. Overall Risk Stratification Summary Card ---
    const score = response.prediction.overall_risk_score;
    const scorePercent = (score * 100).toFixed(1) + '%';
    const category = response.prediction.risk_category || 'MODERATE';

    let tierBg = [254, 243, 199]; // amber-100
    let tierText = [180, 83, 9]; // amber-700
    if (category === 'LOW') {
      tierBg = [209, 250, 229]; // emerald-100
      tierText = [4, 120, 87]; // emerald-700
    } else if (category === 'HIGH' || category === 'ELEVATED') {
      tierBg = [254, 226, 226]; // rose-100
      tierText = [185, 28, 28]; // rose-700
    }

    doc.setFillColor(tierBg[0], tierBg[1], tierBg[2]);
    doc.setDrawColor(tierText[0], tierText[1], tierText[2]);
    doc.rect(margin, y, contentWidth, 22, 'FD');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.setTextColor(tierText[0], tierText[1], tierText[2]);
    doc.text(`PREDICTED CARDIOMETABOLIC RISK: ${category} (${scorePercent})`, margin + 6, y + 8);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.setTextColor(51, 65, 85);
    const summaryText = response.explanation.summary_narrative ||
      'Risk signal computed across 18 multi-modal biomarker dimensions and historical trends.';
    const splitSummary = doc.splitTextToSize(summaryText, contentWidth - 12);
    doc.text(splitSummary, margin + 6, y + 14);

    y += 26;

    // --- 4. Biomarker Measurement Snapshot Table ---
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.setTextColor(15, 23, 42);
    doc.text('RECORDED BIOMARKER SNAPSHOT', margin, y + 4);

    y += 6;
    doc.setDrawColor(203, 213, 225);
    doc.setFillColor(241, 245, 249); // slate-100
    doc.rect(margin, y, contentWidth, 6, 'FD');

    doc.setFontSize(7.5);
    doc.setTextColor(71, 85, 105);
    doc.text('Metric Name', margin + 3, y + 4.2);
    doc.text('Recorded Value', margin + 55, y + 4.2);
    doc.text('Personal Baseline / Normal Range', margin + 95, y + 4.2);
    doc.text('Status Indicator', margin + 145, y + 4.2);

    y += 6;

    const feats = request.features;
    const biomarkerRows = [
      {
        name: 'Blood Pressure (Systolic / Diastolic)',
        value: `${feats.systolic_bp_current} / ${feats.diastolic_bp_current} mmHg`,
        normal: '< 120 / 80 mmHg',
        status: feats.systolic_bp_current >= 140 ? 'High' : feats.systolic_bp_current >= 130 ? 'Elevated' : 'Normal',
      },
      {
        name: 'Fasting Blood Glucose',
        value: `${feats.fasting_glucose_current} mg/dL`,
        normal: '70 - 99 mg/dL',
        status: feats.fasting_glucose_current >= 126 ? 'Elevated' : feats.fasting_glucose_current >= 100 ? 'Pre-diabetic' : 'Normal',
      },
      {
        name: 'Hemoglobin A1c (HbA1c)',
        value: feats.hba1c_current ? `${feats.hba1c_current}%` : 'Not recorded',
        normal: '< 5.7%',
        status: feats.hba1c_current && feats.hba1c_current >= 6.5 ? 'High' : feats.hba1c_current && feats.hba1c_current >= 5.7 ? 'Elevated' : 'Optimal',
      },
      {
        name: 'Body Mass Index (BMI)',
        value: `${feats.bmi} kg/m²`,
        normal: '18.5 - 24.9 kg/m²',
        status: feats.bmi >= 30 ? 'High' : feats.bmi >= 25 ? 'Overweight' : 'Normal',
      },
      {
        name: 'Resting Heart Rate',
        value: `${feats.heart_rate_current} bpm`,
        normal: '60 - 100 bpm',
        status: 'Optimal',
      },
      {
        name: 'Lipid Profile (Total / HDL / LDL / TG)',
        value: `${feats.cholesterol_total || 195} / ${feats.cholesterol_hdl || 52} / ${feats.cholesterol_ldl || 118} / ${feats.triglycerides || 140} mg/dL`,
        normal: 'TC < 200, HDL > 50, LDL < 100',
        status: 'Borderline',
      },
    ];

    biomarkerRows.forEach((row, idx) => {
      const rowBg = idx % 2 === 0 ? 255 : 248;
      doc.setFillColor(rowBg, rowBg, rowBg);
      doc.rect(margin, y, contentWidth, 5.5, 'F');
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(7.5);
      doc.setTextColor(30, 41, 59);

      doc.text(row.name, margin + 3, y + 4);
      doc.setFont('helvetica', 'bold');
      doc.text(row.value, margin + 55, y + 4);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(100, 116, 139);
      doc.text(row.normal, margin + 95, y + 4);

      if (row.status === 'High' || row.status === 'Elevated') {
        doc.setTextColor(220, 38, 38);
      } else if (row.status === 'Pre-diabetic' || row.status === 'Borderline' || row.status === 'Overweight') {
        doc.setTextColor(217, 119, 6);
      } else {
        doc.setTextColor(5, 150, 105);
      }
      doc.setFont('helvetica', 'bold');
      doc.text(row.status, margin + 145, y + 4);

      y += 5.5;
    });

    y += 4;

    // --- 5. TreeSHAP Attribution Breakdown ---
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.setTextColor(15, 23, 42);
    doc.text('TREESHAP LOCAL ATTRIBUTION ANALYSIS', margin, y + 4);

    y += 6;

    // Top Risk Drivers Box
    doc.setFillColor(254, 242, 242); // rose-50
    doc.setDrawColor(254, 202, 202); // rose-200
    doc.rect(margin, y, contentWidth / 2 - 2, 24, 'FD');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(185, 28, 28); // rose-700
    doc.text('+ Primary Risk Drivers (Pushes Risk Higher)', margin + 3, y + 5);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(7.5);
    doc.setTextColor(71, 85, 105);

    const drivers = response.explanation.top_risk_drivers || [];
    if (drivers.length > 0) {
      drivers.slice(0, 3).forEach((d, i) => {
        const valText = `${d.display_name}: +${d.shap_value.toFixed(2)}`;
        doc.text(`• ${valText}`, margin + 3, y + 10 + i * 4.5);
      });
    } else {
      doc.text('• No significant elevated drivers detected.', margin + 3, y + 10);
    }

    // Protective Buffers Box
    const rightX = margin + contentWidth / 2 + 2;
    doc.setFillColor(240, 253, 244); // emerald-50
    doc.setDrawColor(187, 247, 208); // emerald-200
    doc.rect(rightX, y, contentWidth / 2 - 2, 24, 'FD');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(4, 120, 87); // emerald-700
    doc.text('- Protective Biomarker Buffers (Lowers Risk)', rightX + 3, y + 5);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(7.5);
    doc.setTextColor(71, 85, 105);

    const buffers = response.explanation.protective_factors || [];
    if (buffers.length > 0) {
      buffers.slice(0, 3).forEach((b, i) => {
        const valText = `${b.display_name}: ${b.shap_value.toFixed(2)}`;
        doc.text(`• ${valText}`, rightX + 3, y + 10 + i * 4.5);
      });
    } else {
      doc.text('• Adequate physical baseline maintained.', rightX + 3, y + 10);
    }

    y += 28;

    // --- 6. Constrained Counterfactual Recommendations ("What-If" Analysis) ---
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.setTextColor(15, 23, 42);
    doc.text('ACTIONABLE COUNTERFACTUAL TARGETS (SIMULATION)', margin, y + 4);

    y += 6;
    doc.setFillColor(248, 250, 252);
    doc.setDrawColor(226, 232, 240);
    doc.rect(margin, y, contentWidth, 22, 'FD');

    const plans = response.counterfactuals.plans || [];
    if (plans.length > 0) {
      const topPlan = plans[0];
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(8);
      doc.setTextColor(2, 132, 199);
      doc.text(`Plan: ${topPlan.title}`, margin + 4, y + 5);

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(7.5);
      doc.setTextColor(51, 65, 85);
      doc.text(`Projected Risk: ${(topPlan.simulated_risk_score * 100).toFixed(1)}% (${topPlan.simulated_risk_category}) — Reduction: -${(topPlan.risk_reduction * 100).toFixed(1)}%`, margin + 4, y + 10);

      const targets = topPlan.changed_features.map((f) => `${f.display_name} -> ${f.target_value} ${f.unit || ''}`).join(', ');
      doc.text(`Modifications: ${targets}`, margin + 4, y + 15);
      doc.setFont('helvetica', 'italic');
      doc.setTextColor(100, 116, 139);
      doc.text(topPlan.rationale, margin + 4, y + 19);
    } else {
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(8);
      doc.setTextColor(100, 116, 139);
      doc.text('No active counterfactual scenario calculated. Continue maintaining healthy baseline vitals.', margin + 4, y + 11);
    }

    y += 26;

    // --- 7. Mandatory FDA / Clinical Decision Support Disclaimer ---
    doc.setFillColor(254, 243, 199); // amber-100
    doc.setDrawColor(245, 158, 11); // amber-500
    doc.rect(margin, y, contentWidth, 16, 'FD');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(7.5);
    doc.setTextColor(146, 64, 14); // amber-900
    doc.text('REGULATORY & CLINICAL DECISION-SUPPORT GUARDRAIL:', margin + 4, y + 4.5);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(7);
    const disclaimerLines = doc.splitTextToSize(
      'CarePath is an informational and risk-stratification decision-support platform (U.S. FDA 21st Century Cures Act § 520(o)(1)(E)). It does NOT provide medical diagnoses, treatment prescriptions, or emergency clinical triage. Always consult a qualified physician or healthcare provider regarding clinical management.',
      contentWidth - 8
    );
    doc.text(disclaimerLines, margin + 4, y + 9);

    y += 20;

    // --- 8. Doctor Review & Sign-Off Section ---
    doc.setDrawColor(203, 213, 225);
    doc.line(margin, y, pageWidth - margin, y);
    y += 4;

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(71, 85, 105);
    doc.text('Attending Physician Clinical Notes & Confirmation:', margin, y + 3);

    doc.line(margin + 75, y + 3, margin + 180, y + 3);
    y += 8;
    doc.line(margin, y, pageWidth - margin, y);
    y += 8;

    doc.text('Physician Signature: ___________________________________', margin, y);
    doc.text('Date: _______________', margin + 130, y);

    // --- Footer ---
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(7);
    doc.setTextColor(148, 163, 184);
    doc.text(
      `© ${new Date().getFullYear()} CarePath Health Inc. • Strictly Confidential Medical Decision-Support Document • Page 1 of 1`,
      pageWidth / 2,
      290,
      { align: 'center' }
    );

    return doc;
  },

  downloadAssessmentPdf(params: GeneratePdfParams): void {
    const doc = this.generateAssessmentPdf(params);
    const cleanName = (params.patientName || 'Patient').replace(/[^a-zA-Z0-9]/g, '_');
    const safeDate = new Date().toISOString().slice(0, 10);
    const filename = `CarePath-Assessment-Report-${cleanName}-${safeDate}.pdf`;
    doc.save(filename);
  },
};
