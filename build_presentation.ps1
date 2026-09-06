# PowerShell script to populate INIT'26 Presentation template and export to PDF
$ErrorActionPreference = "Stop"

function Get-Rgb([int]$r, [int]$g, [int]$b) {
    return [int]($r + ($g * 256) + ($b * 65536))
}

function Append-FormattedText($textRange, [string]$text, [string]$fontName, [double]$fontSize, [int]$colorRgb, [bool]$isBold) {
    $inserted = $textRange.InsertAfter($text)
    $inserted.Font.Name = $fontName
    $inserted.Font.Size = $fontSize
    $inserted.Font.Color.RGB = $colorRgb
    $inserted.Font.Bold = $isBold
    return $inserted
}

$COLOR_WHITE = Get-Rgb 255 255 255
$COLOR_CYAN = Get-Rgb 6 182 212        # #06b6d4
$COLOR_GREEN = Get-Rgb 34 197 94       # #22c55e
$COLOR_DARK_BG = Get-Rgb 15 23 42      # #0f172a
$COLOR_CARD_BG = Get-Rgb 17 24 39      # #111827
$COLOR_CARD_BORDER = Get-Rgb 30 41 59  # #1e293b
$COLOR_BORDER_CYAN = Get-Rgb 14 116 144 # #0e7490
$COLOR_TEXT_BODY = Get-Rgb 226 232 240 # #e2e8f0

$templatePath = "C:\Users\yokes_gysywu9\Downloads\INIT'26 PPT FORMAT.pptx"
$pptxPath = "d:\INIT hackathon\API-SENTINEL\Team_Phoenix_INIT26_Presentation.pptx"
$pdfPath = "d:\INIT hackathon\API-SENTINEL\Team_Phoenix_INIT26_Presentation.pdf"

Write-Host "Creating fresh presentation copy from official INIT'26 template..."
Copy-Item -Path $templatePath -Destination $pptxPath -Force

$pptApp = New-Object -ComObject PowerPoint.Application
$pres = $pptApp.Presentations.Open($pptxPath, $false, $false, $false)

try {
    # ==========================================
    # SLIDE 1: COVER SLIDE
    # ==========================================
    Write-Host "Configuring Slide 1: Cover Slide..."
    $slide1 = $pres.Slides.Item(1)
    
    # Remove "PPT FORMAT" and stray "(" shapes from the template
    for ($idx = $slide1.Shapes.Count; $idx -ge 1; $idx--) {
        $shp = $slide1.Shapes.Item($idx)
        if ($shp.HasTextFrame -and $shp.TextFrame.HasText) {
            $txt = $shp.TextFrame.TextRange.Text.Trim()
            if ($txt -eq "PPT FORMAT" -or $txt -eq "(" -or $txt -like "*PPT FORMAT*") {
                Write-Host "Deleting shape: $txt"
                $shp.Delete()
            }
        }
    }

    $badgeWidth = 850
    $badgeLeft = [int](($pres.PageSetup.SlideWidth - $badgeWidth) / 2)
    $coverBadge = $slide1.Shapes.AddShape(5, $badgeLeft, 590, $badgeWidth, 52)
    $coverBadge.Fill.Solid()
    $coverBadge.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $coverBadge.Line.ForeColor.RGB = $COLOR_GREEN
    $coverBadge.Line.Weight = 2
    $tr1 = $coverBadge.TextFrame.TextRange
    Append-FormattedText $tr1 "TEAM: PHOENIX   |   TRACK: CYBERSECURITY   |   PROJECT: API SENTINEL" "Antonio Bold" 18 $COLOR_WHITE $true | Out-Null
    $coverBadge.TextFrame.HorizontalAnchor = 2 # Center

    # ==========================================
    # SLIDE 2: TITLE SLIDE (SLIDE 1)
    # ==========================================
    Write-Host "Configuring Slide 2: Title Slide..."
    $slide2 = $pres.Slides.Item(2)
    if ($slide2.Shapes.Count -ge 2) {
        $slide2.Shapes.Item(2).Delete()
    }

    $titleCard = $slide2.Shapes.AddShape(5, 115, 140, 1255, 595)
    $titleCard.Fill.Solid()
    $titleCard.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $titleCard.Line.ForeColor.RGB = $COLOR_GREEN
    $titleCard.Line.Weight = 2

    $tf2 = $titleCard.TextFrame
    $tf2.MarginLeft = 40
    $tf2.MarginRight = 40
    $tf2.MarginTop = 30
    $tf2.MarginBottom = 25
    $tr2 = $tf2.TextRange

    Append-FormattedText $tr2 "TRACK : `r" "Antonio Bold" 24 $COLOR_GREEN $true | Out-Null
    Append-FormattedText $tr2 "Cybersecurity`r`r" "Segoe UI" 20 $COLOR_WHITE $true | Out-Null

    Append-FormattedText $tr2 "PROBLEM STATEMENT : `r" "Antonio Bold" 24 $COLOR_GREEN $true | Out-Null
    $probText = "Modern cloud and microservice architectures rely extensively on distributed REST APIs. Traditional WAFs and perimeter firewalls lack runtime context and fail to inspect dynamic request payloads in real time. Consequently, organizations suffer severe data exfiltration, credential abuse, account takeovers, and silent privilege escalation from OWASP API Top 10 vulnerabilities (including SQL injection, BOLA/IDOR object enumeration, sensitive token exposure, and automated brute-force scraping).`r`r"
    Append-FormattedText $tr2 $probText "Segoe UI" 16 $COLOR_TEXT_BODY $false | Out-Null

    Append-FormattedText $tr2 "TEAM NAME : `r" "Antonio Bold" 24 $COLOR_GREEN $true | Out-Null
    Append-FormattedText $tr2 "Phoenix`r`r" "Segoe UI" 20 $COLOR_WHITE $true | Out-Null

    Append-FormattedText $tr2 "IDEA TITLE : `r" "Antonio Bold" 24 $COLOR_GREEN $true | Out-Null
    $ideaText = "API Sentinel - Autonomous Real-Time API Security, Threat Detection & Zero-Trust Policy Enforcement Platform"
    Append-FormattedText $tr2 $ideaText "Segoe UI" 20 $COLOR_CYAN $true | Out-Null

    # ==========================================
    # SLIDE 3: PROPOSED SOLUTION (SLIDE 2)
    # ==========================================
    Write-Host "Configuring Slide 3: Proposed Solution..."
    $slide3 = $pres.Slides.Item(3)
    
    $cardWidth = 395
    $cardGap = 35
    $startX = 115
    $cardY = 140
    $cardHeight = 590

    # Card 1: Ingestion & Interception
    $c1 = $slide3.Shapes.AddShape(5, $startX, $cardY, $cardWidth, $cardHeight)
    $c1.Fill.Solid(); $c1.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $c1.Line.ForeColor.RGB = $COLOR_CYAN; $c1.Line.Weight = 1.5
    $tf = $c1.TextFrame; $tf.MarginLeft = 25; $tf.MarginRight = 25; $tf.MarginTop = 25
    $tr = $tf.TextRange
    Append-FormattedText $tr "01. INGESTION & INTERCEPTION`r`r" "Antonio Bold" 18 $COLOR_CYAN $true | Out-Null
    Append-FormattedText $tr "Non-Invasive Gateway Interceptor`r`r" "Segoe UI" 18 $COLOR_WHITE $true | Out-Null
    $t1 = "- Transparent HTTP Wrapper: Utilizes CachedBodyHttpServletRequest to safely inspect inbound payload bodies without prematurely consuming the downstream stream.`r`r- Full-Spectrum Inspection: Parses and correlates HTTP headers, URI query params, route variables, and JSON payloads simultaneously.`r`r- Zero Disruption: Operates seamlessly as an upstream reverse proxy or embedded servlet filter with under 5ms latency overhead."
    Append-FormattedText $tr $t1 "Segoe UI" 14 $COLOR_TEXT_BODY $false | Out-Null

    # Card 2: Threat Detection
    $c2 = $slide3.Shapes.AddShape(5, $startX + $cardWidth + $cardGap, $cardY, $cardWidth, $cardHeight)
    $c2.Fill.Solid(); $c2.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $c2.Line.ForeColor.RGB = $COLOR_GREEN; $c2.Line.Weight = 1.5
    $tf = $c2.TextFrame; $tf.MarginLeft = 25; $tf.MarginRight = 25; $tf.MarginTop = 25
    $tr = $tf.TextRange
    Append-FormattedText $tr "02. THREAT DETECTION & SCORING`r`r" "Antonio Bold" 18 $COLOR_GREEN $true | Out-Null
    Append-FormattedText $tr "Multi-Vector Heuristic Engine`r`r" "Segoe UI" 18 $COLOR_WHITE $true | Out-Null
    $t2 = "- OWASP Top 10 Coverage: Modular detectors scan for SQL Injection, XSS payloads, Credential Abuse, and Sensitive Data Exposure.`r`r- IDOR / BOLA Discovery: Sequential path anomaly detection flags automated resource harvesting and parameter enumeration.`r`r- Dynamic 0-100 Scoring: Weights vector confidence, historical IP reputation, and payload lethality to produce an instant threat score."
    Append-FormattedText $tr $t2 "Segoe UI" 14 $COLOR_TEXT_BODY $false | Out-Null

    # Card 3: Autonomous Mitigation
    $c3 = $slide3.Shapes.AddShape(5, $startX + ($cardWidth + $cardGap) * 2, $cardY, $cardWidth, $cardHeight)
    $c3.Fill.Solid(); $c3.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $c3.Line.ForeColor.RGB = $COLOR_CYAN; $c3.Line.Weight = 1.5
    $tf = $c3.TextFrame; $tf.MarginLeft = 25; $tf.MarginRight = 25; $tf.MarginTop = 25
    $tr = $tf.TextRange
    Append-FormattedText $tr "03. MITIGATION & OBSERVABILITY`r`r" "Antonio Bold" 18 $COLOR_CYAN $true | Out-Null
    Append-FormattedText $tr "Zero-Trust Policy Enforcement`r`r" "Segoe UI" 18 $COLOR_WHITE $true | Out-Null
    $t3 = "- Autonomous Response: Instant verdict determination - ALLOWED, RATE_LIMITED (HTTP 429), or BLOCKED (HTTP 403 Forbidden).`r`r- Perimeter IP Isolation: Dynamically quarantines aggressive IPs at the gateway perimeter before microservices are impacted.`r`r- Real-Time SOC Telemetry: Next.js operations console with live threat feeds, forensic payload audits, and interactive attack simulator."
    Append-FormattedText $tr $t3 "Segoe UI" 14 $COLOR_TEXT_BODY $false | Out-Null

    # ==========================================
    # SLIDE 4: TECHNICAL APPROACH (SLIDE 3)
    # ==========================================
    Write-Host "Configuring Slide 4: Technical Approach..."
    $slide4 = $pres.Slides.Item(4)
    
    $gridW = 610
    $gridH = 280
    $col1X = 115
    $col2X = 760
    $row1Y = 140
    $row2Y = 445

    # Tech 1: Frontend SOC
    $t1 = $slide4.Shapes.AddShape(5, $col1X, $row1Y, $gridW, $gridH)
    $t1.Fill.Solid(); $t1.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $t1.Line.ForeColor.RGB = $COLOR_CYAN; $t1.Line.Weight = 1.5
    $tf = $t1.TextFrame; $tf.MarginLeft = 25; $tf.MarginRight = 25; $tf.MarginTop = 20
    $tr = $tf.TextRange
    Append-FormattedText $tr "FRONTEND : SECURITY OPERATIONS CENTER (SOC)`r" "Antonio Bold" 18 $COLOR_CYAN $true | Out-Null
    $desc1 = "- Stack: Next.js 14 (App Router), React 18, TypeScript, Tailwind CSS, Lucide Icons.`r- Live Telemetry: Sub-second telemetry aggregation displaying active threats, request verdicts, risk trends, and endpoints.`r- Live Production Deployment: Hosted on Vercel Edge Network at https://apisentinel-psi.vercel.app`r- Interactive Console: Full audit exploration, policy controls, and offensive attack simulator."
    Append-FormattedText $tr $desc1 "Segoe UI" 13.5 $COLOR_TEXT_BODY $false | Out-Null

    # Tech 2: Backend Gateway
    $t2 = $slide4.Shapes.AddShape(5, $col2X, $row1Y, $gridW, $gridH)
    $t2.Fill.Solid(); $t2.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $t2.Line.ForeColor.RGB = $COLOR_GREEN; $t2.Line.Weight = 1.5
    $tf = $t2.TextFrame; $tf.MarginLeft = 25; $tf.MarginRight = 25; $tf.MarginTop = 20
    $tr = $tf.TextRange
    Append-FormattedText $tr "BACKEND : ZERO-TRUST API SECURITY GATEWAY`r" "Antonio Bold" 18 $COLOR_GREEN $true | Out-Null
    $desc2 = "- Stack: Java 17, Spring Boot 3.2, Spring Security 6 (Stateless Zero-Trust Filter Chain).`r- Resilient Architecture: HikariCP connection pooling, graceful shutdowns, and Actuator health probes.`r- Cloud Deployment: Containerized multi-stage Docker build deployed on Render at https://api-backend-wc8m.onrender.com`r- Performance: Non-blocking in-memory evaluation with under 5ms processing latency per request."
    Append-FormattedText $tr $desc2 "Segoe UI" 13.5 $COLOR_TEXT_BODY $false | Out-Null

    # Tech 3: Pipeline
    $t3 = $slide4.Shapes.AddShape(5, $col1X, $row2Y, $gridW, $gridH)
    $t3.Fill.Solid(); $t3.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $t3.Line.ForeColor.RGB = $COLOR_GREEN; $t3.Line.Weight = 1.5
    $tf = $t3.TextFrame; $tf.MarginLeft = 25; $tf.MarginRight = 25; $tf.MarginTop = 20
    $tr = $tf.TextRange
    Append-FormattedText $tr "CORE DETECTION & SCORING PIPELINE`r" "Antonio Bold" 18 $COLOR_GREEN $true | Out-Null
    $desc3 = "- Interception Filter: Intercepts raw byte streams and constructs thread-safe ApiRequestContext.`r- Modular Detectors: InjectionDetector (SQLi/XSS), EnumerationDetector (BOLA), AuthenticationAbuseDetector, RateAbuseDetector, SensitiveDataExposureDetector.`r- Scoring Engine: ThreatScoringEngine evaluates weighted vector risk into deterministic score (0-100).`r- Enforcement Engine: DefaultPolicyEngine matches active rules and returns instant verdict."
    Append-FormattedText $tr $desc3 "Segoe UI" 13.5 $COLOR_TEXT_BODY $false | Out-Null

    # Tech 4: Database & Monorepo
    $t4 = $slide4.Shapes.AddShape(5, $col2X, $row2Y, $gridW, $gridH)
    $t4.Fill.Solid(); $t4.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $t4.Line.ForeColor.RGB = $COLOR_CYAN; $t4.Line.Weight = 1.5
    $tf = $t4.TextFrame; $tf.MarginLeft = 25; $tf.MarginRight = 25; $tf.MarginTop = 20
    $tr = $tf.TextRange
    Append-FormattedText $tr "PERSISTENCE & MONOREPO CLOUD ARCHITECTURE`r" "Antonio Bold" 18 $COLOR_CYAN $true | Out-Null
    $desc4 = "- Cloud Database: Managed PostgreSQL database on Render storing security incidents, request audit logs, endpoints, and firewall policies.`r- Single GitHub Monorepo: Houses /backend, /frontend, /database, and orchestration in one repo.`r- Local Development: docker-compose.yml runs complete local MySQL/Spring/Next.js environment.`r- Cross-Origin Security: Fine-grained CORS controls securing browser clients and API gateways."
    Append-FormattedText $tr $desc4 "Segoe UI" 13.5 $COLOR_TEXT_BODY $false | Out-Null

    # ==========================================
    # SLIDE 5: USER FLOW (SLIDE 4)
    # ==========================================
    Write-Host "Configuring Slide 5: User Flow..."
    $slide5 = $pres.Slides.Item(5)
    
    $stepW = 395
    $stepH = 275
    $sX = 115
    $gapX = 35
    $r1Y = 140
    $r2Y = 445

    $steps = @(
        @{ Num = "STEP 1"; Title = "Inbound Request Ingestion"; Desc = "Client or attacker issues HTTP API call (GET, POST, PUT, DELETE) targeting protected microservice endpoints."; Color = $COLOR_CYAN; X = $sX; Y = $r1Y },
        @{ Num = "STEP 2"; Title = "Deep Payload Caching"; Desc = "TrafficInterceptionFilter wraps stream using CachedBodyHttpServletRequest and extracts headers, URI parameters, and body."; Color = $COLOR_GREEN; X = $sX + $stepW + $gapX; Y = $r1Y },
        @{ Num = "STEP 3"; Title = "Multi-Vector Heuristics"; Desc = "Engine executes parallel modular detectors scanning for SQL injection tokens, XSS scripts, token leakage, and BOLA enumeration patterns."; Color = $COLOR_CYAN; X = $sX + ($stepW + $gapX) * 2; Y = $r1Y },
        @{ Num = "STEP 4"; Title = "Dynamic Threat Scoring"; Desc = "ThreatScoringEngine aggregates detection signals and evaluates a composite 0-100 severity score with recommended action."; Color = $COLOR_GREEN; X = $sX; Y = $r2Y },
        @{ Num = "STEP 5"; Title = "Automated Policy Action"; Desc = "PolicyEngine decides verdict: ALLOWED (forwarded to downstream handler), RATE_LIMITED (HTTP 429), or BLOCKED (HTTP 403 Forbidden)."; Color = $COLOR_CYAN; X = $sX + $stepW + $gapX; Y = $r2Y },
        @{ Num = "STEP 6"; Title = "Forensic Log & SOC Update"; Desc = "Request audit details and forensic evidence are persisted to PostgreSQL and pushed live to the SOC Dashboard in real time."; Color = $COLOR_GREEN; X = $sX + ($stepW + $gapX) * 2; Y = $r2Y }
    )

    foreach ($st in $steps) {
        $box = $slide5.Shapes.AddShape(5, $st.X, $st.Y, $stepW, $stepH)
        $box.Fill.Solid(); $box.Fill.ForeColor.RGB = $COLOR_CARD_BG
        $box.Line.ForeColor.RGB = $st.Color; $box.Line.Weight = 1.5
        $tf = $box.TextFrame; $tf.MarginLeft = 25; $tf.MarginRight = 25; $tf.MarginTop = 20
        $tr = $tf.TextRange
        Append-FormattedText $tr "$($st.Num) : $($st.Title)`r`r" "Antonio Bold" 17 $st.Color $true | Out-Null
        Append-FormattedText $tr $st.Desc "Segoe UI" 14 $COLOR_TEXT_BODY $false | Out-Null
    }

    # ==========================================
    # SLIDE 6: PROJECT SNAPSHOTS (SLIDE 5)
    # ==========================================
    Write-Host "Configuring Slide 6: Project Snapshots..."
    $slide6 = $pres.Slides.Item(6)

    $img1Path = "d:\INIT hackathon\API-SENTINEL\screenshot_dashboard.png"
    $img2Path = "d:\INIT hackathon\API-SENTINEL\screenshot_simulator.png"

    if (Test-Path $img1Path) {
        $cImg1 = $slide6.Shapes.AddShape(5, 115, 140, 610, 480)
        $cImg1.Fill.Solid(); $cImg1.Fill.ForeColor.RGB = $COLOR_CARD_BG
        $cImg1.Line.ForeColor.RGB = $COLOR_CYAN; $cImg1.Line.Weight = 1.5
        
        $pic1 = $slide6.Shapes.AddPicture($img1Path, 0, 1, 125, 150, 590, 345)
        
        $cap1 = $slide6.Shapes.AddTextbox(1, 125, 505, 590, 110)
        $tr = $cap1.TextFrame.TextRange
        Append-FormattedText $tr "Security Operations Center (SOC) Dashboard`r" "Antonio Bold" 16 $COLOR_CYAN $true | Out-Null
        Append-FormattedText $tr "Real-time traffic health ratios, threat severity composition, average risk scores, and live incident telemetry feed connected to Render PostgreSQL." "Segoe UI" 12.5 $COLOR_TEXT_BODY $false | Out-Null
    }

    if (Test-Path $img2Path) {
        $cImg2 = $slide6.Shapes.AddShape(5, 760, 140, 610, 480)
        $cImg2.Fill.Solid(); $cImg2.Fill.ForeColor.RGB = $COLOR_CARD_BG
        $cImg2.Line.ForeColor.RGB = $COLOR_GREEN; $cImg2.Line.Weight = 1.5
        
        $pic2 = $slide6.Shapes.AddPicture($img2Path, 0, 1, 770, 150, 590, 345)
        
        $cap2 = $slide6.Shapes.AddTextbox(1, 770, 505, 590, 110)
        $tr = $cap2.TextFrame.TextRange
        Append-FormattedText $tr "Interactive Attack Simulator & Forensic Inspector`r" "Antonio Bold" 16 $COLOR_GREEN $true | Out-Null
        Append-FormattedText $tr "Simulates OWASP attack vectors (SQLi, IDOR/BOLA, Brute Force) against live APIs; verifies instantaneous detection, threat scoring, and gateway mitigation." "Segoe UI" 12.5 $COLOR_TEXT_BODY $false | Out-Null
    }

    # Bottom Live URL bar
    $urlBar = $slide6.Shapes.AddShape(5, 115, 635, 1255, 90)
    $urlBar.Fill.Solid(); $urlBar.Fill.ForeColor.RGB = $COLOR_DARK_BG
    $urlBar.Line.ForeColor.RGB = $COLOR_BORDER_CYAN; $urlBar.Line.Weight = 1.5
    $tf = $urlBar.TextFrame; $tf.MarginLeft = 25; $tf.MarginTop = 15
    $tr = $tf.TextRange
    Append-FormattedText $tr "LIVE DEPLOYED DEMO LINKS (CLICKABLE IN SUBMISSION) :`r" "Antonio Bold" 14 $COLOR_GREEN $true | Out-Null
    $linkText = "Frontend: https://apisentinel-psi.vercel.app   |   Backend API: https://api-backend-wc8m.onrender.com   |   GitHub: https://github.com/Yokeshwaran03-2005/API-SENTINEL"
    Append-FormattedText $tr $linkText "Segoe UI" 13 $COLOR_WHITE $true | Out-Null

    # ==========================================
    # SLIDE 7: FEASIBILITY AND IMPACT (SLIDE 6)
    # ==========================================
    Write-Host "Configuring Slide 7: Feasibility & Impact..."
    $slide7 = $pres.Slides.Item(7)

    $hCardW = 1255
    $hCardH = 180
    $hStartX = 115
    $hY1 = 140
    $hY2 = 340
    $hY3 = 540

    # Feasibility
    $f1 = $slide7.Shapes.AddShape(5, $hStartX, $hY1, $hCardW, $hCardH)
    $f1.Fill.Solid(); $f1.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $f1.Line.ForeColor.RGB = $COLOR_CYAN; $f1.Line.Weight = 1.5
    $tf = $f1.TextFrame; $tf.MarginLeft = 30; $tf.MarginRight = 30; $tf.MarginTop = 15
    $tr = $tf.TextRange
    Append-FormattedText $tr "TECHNICAL FEASIBILITY & CLOUD-NATIVE SCALABILITY`r" "Antonio Bold" 18 $COLOR_CYAN $true | Out-Null
    $fText1 = "- High Efficiency: Sub-5ms latency overhead per evaluation ensures near-zero performance degradation for high-traffic APIs.`r- Flexible Deployment: Can run as an independent API Gateway reverse proxy, a sidecar container in Kubernetes pods, or an embedded Spring filter.`r- Containerized & Portable: Packaged via multi-stage Docker builds; runs on AWS, Render, Google Cloud, or on-premise infrastructure."
    Append-FormattedText $tr $fText1 "Segoe UI" 13.5 $COLOR_TEXT_BODY $false | Out-Null

    # Viability
    $f2 = $slide7.Shapes.AddShape(5, $hStartX, $hY2, $hCardW, $hCardH)
    $f2.Fill.Solid(); $f2.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $f2.Line.ForeColor.RGB = $COLOR_GREEN; $f2.Line.Weight = 1.5
    $tf = $f2.TextFrame; $tf.MarginLeft = 30; $tf.MarginRight = 30; $tf.MarginTop = 15
    $tr = $tf.TextRange
    Append-FormattedText $tr "COMMERCIAL VIABILITY & MARKET ADOPTION`r" "Antonio Bold" 18 $COLOR_GREEN $true | Out-Null
    $fText2 = "- Exploding Market Demand: API attacks surged over 400% YoY, rendering standard IP-based firewalls obsolete against application-layer abuse.`r- Cost-Effective Alternative: Replaces bulky multi-thousand dollar enterprise WAF appliances with lightweight, software-defined API defense.`r- Sector Relevance: Direct applicability to Fintech (PSD2/Open Banking), Healthcare (HIPAA), E-Commerce, and SaaS API providers."
    Append-FormattedText $tr $fText2 "Segoe UI" 13.5 $COLOR_TEXT_BODY $false | Out-Null

    # Impact
    $f3 = $slide7.Shapes.AddShape(5, $hStartX, $hY3, $hCardW, $hCardH)
    $f3.Fill.Solid(); $f3.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $f3.Line.ForeColor.RGB = $COLOR_CYAN; $f3.Line.Weight = 1.5
    $tf = $f3.TextFrame; $tf.MarginLeft = 30; $tf.MarginRight = 30; $tf.MarginTop = 15
    $tr = $tf.TextRange
    Append-FormattedText $tr "MEASURABLE IMPACT & SOC EMPOWERMENT`r" "Antonio Bold" 18 $COLOR_CYAN $true | Out-Null
    $fText3 = "- MTTD & MTTR Drastically Reduced: Threat detection and automated mitigation drop from hours of manual log triage to under 100 milliseconds.`r- 100% Autonomous Perimeter Defense: Prevents data exfiltration and unauthorized resource tampering before reaching backend services.`r- Full Compliance Trail: Immutable audit logs in PostgreSQL simplify regulatory audits (SOC2 Type II, ISO 27001, and GDPR)."
    Append-FormattedText $tr $fText3 "Segoe UI" 13.5 $COLOR_TEXT_BODY $false | Out-Null

    # ==========================================
    # SLIDE 8: REFERENCES (SLIDE 7)
    # ==========================================
    Write-Host "Configuring Slide 8: Project Links (Live Demo & GitHub)..."
    $slide8 = $pres.Slides.Item(8)

    for ($idx = $slide8.Shapes.Count; $idx -ge 2; $idx--) {
        $slide8.Shapes.Item($idx).Delete()
    }

    $cardW = 605
    $cardGap = 45
    $cardStartX = 115
    $cardY = 150
    $cardH = 570

    # ------------------------------------------
    # Card 1: Live Demo (Left)
    # ------------------------------------------
    $c1 = $slide8.Shapes.AddShape(5, $cardStartX, $cardY, $cardW, $cardH)
    $c1.Fill.Solid(); $c1.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $c1.Line.ForeColor.RGB = $COLOR_GREEN; $c1.Line.Weight = 2
    
    # Header 1
    $hBox1 = $slide8.Shapes.AddTextbox(1, $cardStartX + 30, $cardY + 25, 545, 65)
    $tr = $hBox1.TextFrame.TextRange
    $tr.ParagraphFormat.Alignment = 1
    Append-FormattedText $tr "LIVE DEMO PLATFORM`r" "Antonio Bold" 24 $COLOR_GREEN $true | Out-Null
    Append-FormattedText $tr "Production Security Operations Center (SOC) on Vercel" "Segoe UI" 13.5 $COLOR_CYAN $true | Out-Null

    # Link Button 1
    $btn1 = $slide8.Shapes.AddShape(5, $cardStartX + 30, $cardY + 100, 545, 50)
    $btn1.Fill.Solid(); $btn1.Fill.ForeColor.RGB = $COLOR_DARK_BG
    $btn1.Line.ForeColor.RGB = $COLOR_GREEN; $btn1.Line.Weight = 1.5
    $tr = $btn1.TextFrame.TextRange
    $tr.ParagraphFormat.Alignment = 2
    Append-FormattedText $tr "https://apisentinel-psi.vercel.app" "Segoe UI" 16 $COLOR_WHITE $true | Out-Null
    try {
        $btn1.ActionSettings.Item(1).Action = 7
        $btn1.ActionSettings.Item(1).Hyperlink.Address = "https://apisentinel-psi.vercel.app"
    } catch {}

    # Features 1
    $fBox1 = $slide8.Shapes.AddTextbox(1, $cardStartX + 30, $cardY + 165, 545, 380)
    $tr = $fBox1.TextFrame.TextRange
    $tr.ParagraphFormat.Alignment = 1
    Append-FormattedText $tr "PLATFORM CAPABILITIES:`r`r" "Antonio Bold" 15 $COLOR_GREEN $true | Out-Null
    $demoFeatures = "- Real-Time SOC Dashboard: Instant visibility into active threats, request verdicts, risk scores, and traffic health.`r`r- Autonomous Policy Mitigation: Dynamic zero-trust blocking (HTTP 403) and rate limiting (HTTP 429).`r`r- Interactive Attack Simulator: Test live OWASP payloads (SQLi, IDOR/BOLA, Brute Force) against protected endpoints.`r`r- Forensic Request Inspector: Deep inspection of headers, masked credentials, and cached payload bodies.`r`r- Cloud Deployment: High-availability edge hosting on Vercel."
    Append-FormattedText $tr $demoFeatures "Segoe UI" 13.5 $COLOR_TEXT_BODY $false | Out-Null

    # ------------------------------------------
    # Card 2: GitHub Repository (Right)
    # ------------------------------------------
    $c2 = $slide8.Shapes.AddShape(5, $cardStartX + $cardW + $cardGap, $cardY, $cardW, $cardH)
    $c2.Fill.Solid(); $c2.Fill.ForeColor.RGB = $COLOR_CARD_BG
    $c2.Line.ForeColor.RGB = $COLOR_CYAN; $c2.Line.Weight = 2

    # Header 2
    $hBox2 = $slide8.Shapes.AddTextbox(1, $cardStartX + $cardW + $cardGap + 30, $cardY + 25, 545, 65)
    $tr = $hBox2.TextFrame.TextRange
    $tr.ParagraphFormat.Alignment = 1
    Append-FormattedText $tr "GITHUB REPOSITORY`r" "Antonio Bold" 24 $COLOR_CYAN $true | Out-Null
    Append-FormattedText $tr "Public Monorepo & Implementation Source Code" "Segoe UI" 13.5 $COLOR_GREEN $true | Out-Null

    # Link Button 2
    $btn2 = $slide8.Shapes.AddShape(5, $cardStartX + $cardW + $cardGap + 30, $cardY + 100, 545, 50)
    $btn2.Fill.Solid(); $btn2.Fill.ForeColor.RGB = $COLOR_DARK_BG
    $btn2.Line.ForeColor.RGB = $COLOR_CYAN; $btn2.Line.Weight = 1.5
    $tr = $btn2.TextFrame.TextRange
    $tr.ParagraphFormat.Alignment = 2
    Append-FormattedText $tr "https://github.com/Yokeshwaran03-2005/API-SENTINEL" "Segoe UI" 14.5 $COLOR_WHITE $true | Out-Null
    try {
        $btn2.ActionSettings.Item(1).Action = 7
        $btn2.ActionSettings.Item(1).Hyperlink.Address = "https://github.com/Yokeshwaran03-2005/API-SENTINEL"
    } catch {}

    # Features 2
    $fBox2 = $slide8.Shapes.AddTextbox(1, $cardStartX + $cardW + $cardGap + 30, $cardY + 165, 545, 380)
    $tr = $fBox2.TextFrame.TextRange
    $tr.ParagraphFormat.Alignment = 1
    Append-FormattedText $tr "REPOSITORY CONTENTS:`r`r" "Antonio Bold" 15 $COLOR_CYAN $true | Out-Null
    $repoFeatures = "- Unified Monorepo: Houses complete /backend, /frontend, /database, and container orchestration.`r`r- Backend Gateway: Java 17 + Spring Boot 3.2 zero-trust servlet filter chain, heuristics & scoring engine.`r`r- Frontend Console: Next.js 14 App Router, TypeScript, Tailwind CSS, and Lucide icons.`r`r- Comprehensive Documentation: Architecture diagrams, threat model specs, and local Docker Compose setup.`r`r- Official Hackathon Submission: Built for INIT'26 by Team Phoenix."
    Append-FormattedText $tr $repoFeatures "Segoe UI" 13.5 $COLOR_TEXT_BODY $false | Out-Null

    Write-Host "Saving presentation..."
    $pres.Save()
    
    Write-Host "Exporting to PDF: $pdfPath..."
    $pres.SaveAs($pdfPath, 32)
    Write-Host "SUCCESS: Presentation and PDF generated cleanly!"

} finally {
    $pres.Close()
    $pptApp.Quit()
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($pres) | Out-Null
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($pptApp) | Out-Null
}
