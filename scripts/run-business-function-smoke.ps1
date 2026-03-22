param(
    [string]$BaseUrl = 'http://localhost:9999'
)

$ErrorActionPreference = 'Stop'

$baseUrl = $BaseUrl.TrimEnd('/')
$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$scriptRoot = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$repoRoot = (Resolve-Path (Join-Path $scriptRoot '..')).Path
$outDir = Join-Path $repoRoot 'logs\acceptance'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$script:authHeaders = @{}

$results = New-Object System.Collections.Generic.List[object]

function Add-Result {
    param(
        [string]$Point,
        [string]$Case,
        [string]$Method,
        [string]$Path,
        [bool]$Critical = $true,
        [string]$Status = 'FAIL',
        [string]$Detail = ''
    )
    $results.Add([pscustomobject]@{
            point    = $Point
            case     = $Case
            method   = $Method
            path     = $Path
            critical = $Critical
            status   = $Status
            detail   = $Detail
            at       = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
        })
}

function Trim-Text {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) { return '' }
    $s = $Text.Replace("`r", ' ').Replace("`n", ' ')
    if ($s.Length -gt 260) { return $s.Substring(0, 260) + '...' }
    return $s
}

function Invoke-Step {
    param(
        [string]$Point,
        [string]$Case,
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [bool]$Critical = $true
    )
    $url = "$baseUrl$Path"
    try {
        $headers = @{}

        # Attach Authorization header for protected endpoints once login succeeds.
        if ($script:authHeaders.Count -gt 0) {
            $headers = $script:authHeaders
        }

        if ($null -ne $Body) {
            $json = $Body | ConvertTo-Json -Depth 20
            if ($headers.Count -gt 0) {
                $resp = Invoke-RestMethod -Uri $url -Method $Method -Headers $headers -Body $json -ContentType 'application/json; charset=utf-8' -TimeoutSec 30
            } else {
                $resp = Invoke-RestMethod -Uri $url -Method $Method -Body $json -ContentType 'application/json; charset=utf-8' -TimeoutSec 30
            }
        } else {
            if ($headers.Count -gt 0) {
                $resp = Invoke-RestMethod -Uri $url -Method $Method -Headers $headers -TimeoutSec 30
            } else {
                $resp = Invoke-RestMethod -Uri $url -Method $Method -TimeoutSec 30
            }
        }

        if ($null -eq $resp) {
            Add-Result -Point $Point -Case $Case -Method $Method -Path $Path -Critical $Critical -Status 'FAIL' -Detail 'empty response'
            return $null
        }

        if ($resp.PSObject.Properties.Name -contains 'code') {
            $ok = ($resp.code -eq 200)
            Add-Result -Point $Point -Case $Case -Method $Method -Path $Path -Critical $Critical -Status ($(if ($ok) { 'PASS' } else { 'FAIL' })) -Detail ("code=$($resp.code); msg=$(Trim-Text $resp.msg)")
            if ($ok) { return $resp }
            return $null
        }

        Add-Result -Point $Point -Case $Case -Method $Method -Path $Path -Critical $Critical -Status 'PASS' -Detail 'non-envelope response'
        return $resp
    } catch {
        $raw = $_.ErrorDetails.Message
        if ([string]::IsNullOrWhiteSpace($raw)) { $raw = $_.Exception.Message }
        Add-Result -Point $Point -Case $Case -Method $Method -Path $Path -Critical $Critical -Status 'FAIL' -Detail (Trim-Text $raw)
        return $null
    }
}

function Id-Of {
    param([object]$Resp)
    if ($null -eq $Resp -or $null -eq $Resp.data) { return $null }
    if ($Resp.data.PSObject.Properties.Name -contains 'id') { return [long]$Resp.data.id }
    return $null
}

function Skip-Step {
    param(
        [string]$Point,
        [string]$Case,
        [string]$Method,
        [string]$Path,
        [string]$Reason,
        [bool]$Critical = $true
    )
    Add-Result -Point $Point -Case $Case -Method $Method -Path $Path -Critical $Critical -Status 'FAIL' -Detail ("SKIP: $Reason")
}

function Url-Encode([string]$v) { return [uri]::EscapeDataString($v) }

function Try-Login {
    param([string]$Path)
    $url = "$baseUrl$Path"
    try {
        $payload = @{ username = 'admin'; password = '123456' } | ConvertTo-Json
        return Invoke-RestMethod -Uri $url -Method POST -Body $payload -ContentType 'application/json; charset=utf-8' -TimeoutSec 15
    } catch {
        return $null
    }
}

$loginResp = Try-Login -Path '/api/auth/login'
$loginPath = '/api/auth/login'
if ($loginResp -and $loginResp.code -eq 200 -and $loginResp.data -and $loginResp.data.token) {
    $script:authHeaders = @{ Authorization = "Bearer $($loginResp.data.token)" }
    Add-Result -Point 'ENV' -Case 'login-token' -Method 'POST' -Path $loginPath -Status 'PASS' -Detail 'login succeeded'
    try {
        $meResp = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method GET -Headers $script:authHeaders -TimeoutSec 15
        if ($meResp -and $meResp.code -eq 200) {
            Add-Result -Point 'ENV' -Case 'token-check' -Method 'GET' -Path '/api/auth/me' -Status 'PASS' -Detail 'token valid'
        } else {
            Add-Result -Point 'ENV' -Case 'token-check' -Method 'GET' -Path '/api/auth/me' -Status 'FAIL' -Detail 'token check failed'
        }
    } catch {
        $raw = $_.ErrorDetails.Message
        if ([string]::IsNullOrWhiteSpace($raw)) { $raw = $_.Exception.Message }
        Add-Result -Point 'ENV' -Case 'token-check' -Method 'GET' -Path '/api/auth/me' -Status 'FAIL' -Detail (Trim-Text $raw)
    }
} else {
    Add-Result -Point 'ENV' -Case 'login-token' -Method 'POST' -Path '/api/auth/login' -Status 'FAIL' -Detail 'login failed or token missing'
}

# 0) environment
Invoke-Step -Point 'ENV' -Case 'backend-alive' -Method 'GET' -Path '/api/device/list' | Out-Null

# 1) iot base
$productKey = "accept-auto-product-$stamp"
$deviceCode = "accept-auto-device-$stamp"
$deviceName = "auto-device-$stamp"
$now = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')

$p = Invoke-Step -Point 'IOT-PRODUCT' -Case 'add-product' -Method 'POST' -Path '/api/device/product/add' -Body @{
    productKey   = $productKey
    productName  = "auto-product-$stamp"
    protocolCode = 'mqtt-json'
    nodeType     = 1
    dataFormat   = 'JSON'
}
$productId = Id-Of $p
if ($productId) { Invoke-Step -Point 'IOT-PRODUCT' -Case 'get-product' -Method 'GET' -Path "/api/device/product/$productId" | Out-Null } else { Skip-Step -Point 'IOT-PRODUCT' -Case 'get-product' -Method 'GET' -Path '/api/device/product/{id}' -Reason 'productId missing' }

$d = Invoke-Step -Point 'IOT-DEVICE' -Case 'add-device' -Method 'POST' -Path '/api/device/add' -Body @{
    productKey   = $productKey
    deviceName   = $deviceName
    deviceCode   = $deviceCode
    deviceSecret = '123456'
    clientId     = $deviceCode
    username     = $deviceCode
    password     = '123456'
}
$deviceId = Id-Of $d
if ($deviceId) {
    Invoke-Step -Point 'IOT-DEVICE' -Case 'get-device-by-id' -Method 'GET' -Path "/api/device/$deviceId" | Out-Null
    Invoke-Step -Point 'IOT-DEVICE' -Case 'get-device-by-code' -Method 'GET' -Path "/api/device/code/$deviceCode" | Out-Null
    Invoke-Step -Point 'IOT-DEVICE' -Case 'list-device-options' -Method 'GET' -Path '/api/device/list' -Critical $false | Out-Null
} else {
    Skip-Step -Point 'IOT-DEVICE' -Case 'get-device-by-id' -Method 'GET' -Path '/api/device/{id}' -Reason 'deviceId missing'
    Skip-Step -Point 'IOT-DEVICE' -Case 'get-device-by-code' -Method 'GET' -Path '/api/device/code/{code}' -Reason 'deviceId missing'
}

Invoke-Step -Point 'INGEST-HTTP' -Case 'http-report' -Method 'POST' -Path '/api/message/http/report' -Body @{
    protocolCode = 'mqtt-json'
    productKey   = $productKey
    deviceCode   = $deviceCode
    payload      = '{"messageType":"property","properties":{"temperature":26.5,"humidity":68}}'
    topic        = "/sys/$productKey/$deviceCode/thing/property/post"
    clientId     = $deviceCode
    tenantId     = '1'
} | Out-Null
Invoke-Step -Point 'INGEST-HTTP' -Case 'get-properties' -Method 'GET' -Path "/api/device/$deviceCode/properties" | Out-Null
Invoke-Step -Point 'INGEST-HTTP' -Case 'get-message-logs' -Method 'GET' -Path "/api/device/$deviceCode/message-logs" | Out-Null

Invoke-Step -Point 'MQTT-DOWN' -Case 'publish-down' -Method 'POST' -Path '/api/message/mqtt/down/publish' -Body @{
    productKey  = $productKey
    deviceCode  = $deviceCode
    qos         = 1
    commandType = 'property'
    params      = @{ switch = 1; targetTemperature = 23.0; requestId = "auto-down-$stamp" }
} | Out-Null

# 2) alarm center
$alarmCode1 = "AUTO-ALARM-A-$stamp"
$alarmCode2 = "AUTO-ALARM-B-$stamp"
$a1 = Invoke-Step -Point 'ALARM' -Case 'add-alarm-a' -Method 'POST' -Path '/api/alarm/add' -Body @{
    alarmCode      = $alarmCode1
    alarmTitle     = 'auto-alarm-a'
    alarmType      = 'threshold'
    alarmLevel     = 'warning'
    regionId       = 1
    regionName     = 'default-region'
    riskPointId    = 1
    riskPointName  = 'default-risk-point'
    deviceId       = $deviceId
    deviceCode     = $deviceCode
    deviceName     = $deviceName
    metricName     = 'temperature'
    currentValue   = '88.8'
    thresholdValue = '80'
    status         = 0
    triggerTime    = $now
    tenantId       = 1
}
$a1Id = Id-Of $a1
$a2 = Invoke-Step -Point 'ALARM' -Case 'add-alarm-b' -Method 'POST' -Path '/api/alarm/add' -Body @{
    alarmCode      = $alarmCode2
    alarmTitle     = 'auto-alarm-b'
    alarmType      = 'threshold'
    alarmLevel     = 'warning'
    regionId       = 1
    regionName     = 'default-region'
    riskPointId    = 1
    riskPointName  = 'default-risk-point'
    deviceId       = $deviceId
    deviceCode     = $deviceCode
    deviceName     = $deviceName
    metricName     = 'temperature'
    currentValue   = '89.9'
    thresholdValue = '80'
    status         = 0
    triggerTime    = $now
    tenantId       = 1
}
$a2Id = Id-Of $a2
Invoke-Step -Point 'ALARM' -Case 'list-alarms' -Method 'GET' -Path "/api/alarm/list?deviceCode=$deviceCode" | Out-Null
if ($a1Id) {
    Invoke-Step -Point 'ALARM' -Case 'get-alarm' -Method 'GET' -Path "/api/alarm/$a1Id" | Out-Null
    Invoke-Step -Point 'ALARM' -Case 'confirm-alarm' -Method 'POST' -Path "/api/alarm/$a1Id/confirm?confirmUser=1" | Out-Null
    Invoke-Step -Point 'ALARM' -Case 'close-alarm' -Method 'POST' -Path "/api/alarm/$a1Id/close?closeUser=1" | Out-Null
} else {
    Skip-Step -Point 'ALARM' -Case 'get-alarm' -Method 'GET' -Path '/api/alarm/{id}' -Reason 'alarmId missing'
}
if ($a2Id) { Invoke-Step -Point 'ALARM' -Case 'suppress-alarm' -Method 'POST' -Path "/api/alarm/$a2Id/suppress?suppressUser=1" | Out-Null } else { Skip-Step -Point 'ALARM' -Case 'suppress-alarm' -Method 'POST' -Path '/api/alarm/{id}/suppress' -Reason 'alarmId missing' }

# 3) event disposal
$eventCode1 = "AUTO-EVENT-A-$stamp"
$eventCode2 = "AUTO-EVENT-B-$stamp"
$e1 = Invoke-Step -Point 'EVENT' -Case 'add-event-a' -Method 'POST' -Path '/api/event/add' -Body @{
    eventCode      = $eventCode1
    eventTitle     = 'auto-event-a'
    alarmId        = $a1Id
    alarmCode      = $alarmCode1
    alarmLevel     = 'warning'
    riskLevel      = 'warning'
    regionId       = 1
    regionName     = 'default-region'
    riskPointId    = 1
    riskPointName  = 'default-risk-point'
    deviceId       = $deviceId
    deviceCode     = $deviceCode
    deviceName     = $deviceName
    metricName     = 'temperature'
    currentValue   = '90'
    status         = 0
    triggerTime    = $now
    tenantId       = 1
}
$e1Id = Id-Of $e1
$e2 = Invoke-Step -Point 'EVENT' -Case 'add-event-b-feedback-state' -Method 'POST' -Path '/api/event/add' -Body @{
    eventCode      = $eventCode2
    eventTitle     = 'auto-event-b'
    alarmId        = $a2Id
    alarmCode      = $alarmCode2
    alarmLevel     = 'warning'
    riskLevel      = 'warning'
    regionId       = 1
    regionName     = 'default-region'
    riskPointId    = 1
    riskPointName  = 'default-risk-point'
    deviceId       = $deviceId
    deviceCode     = $deviceCode
    deviceName     = $deviceName
    metricName     = 'temperature'
    currentValue   = '91'
    status         = 2
    triggerTime    = $now
    tenantId       = 1
}
$e2Id = Id-Of $e2
Invoke-Step -Point 'EVENT' -Case 'list-events' -Method 'GET' -Path "/api/event/list?deviceCode=$deviceCode" | Out-Null
if ($e1Id) {
    Invoke-Step -Point 'EVENT' -Case 'get-event' -Method 'GET' -Path "/api/event/$e1Id" | Out-Null
    Invoke-Step -Point 'EVENT' -Case 'dispatch-event' -Method 'POST' -Path "/api/event/$e1Id/dispatch?dispatchUser=1&receiveUser=1" | Out-Null
} else {
    Skip-Step -Point 'EVENT' -Case 'get-event' -Method 'GET' -Path '/api/event/{id}' -Reason 'eventId missing'
}
$w = Invoke-Step -Point 'EVENT' -Case 'list-work-orders' -Method 'GET' -Path '/api/event/work-orders?receiveUser=1'
$workOrderId = $null
if ($w -and $w.data) {
    $target = $w.data | Where-Object { $_.eventCode -eq $eventCode1 } | Select-Object -First 1
    if (-not $target) { $target = $w.data | Select-Object -First 1 }
    if ($target) { $workOrderId = [long]$target.id }
}
if ($workOrderId) {
    Invoke-Step -Point 'EVENT' -Case 'receive-work-order' -Method 'POST' -Path "/api/event/work-orders/$workOrderId/receive?receiveUser=1" | Out-Null
    Invoke-Step -Point 'EVENT' -Case 'start-work-order' -Method 'POST' -Path "/api/event/work-orders/$workOrderId/start?receiveUser=1" | Out-Null
    Invoke-Step -Point 'EVENT' -Case 'complete-work-order' -Method 'POST' -Path "/api/event/work-orders/$workOrderId/complete?feedback=$(Url-Encode 'auto-complete')&photos=$(Url-Encode '[]')" | Out-Null
} else {
    Skip-Step -Point 'EVENT' -Case 'receive-work-order' -Method 'POST' -Path '/api/event/work-orders/{id}/receive' -Reason 'workOrderId missing'
}
if ($e2Id) { Invoke-Step -Point 'EVENT' -Case 'event-feedback' -Method 'POST' -Path "/api/event/$e2Id/feedback?feedback=$(Url-Encode 'auto-feedback')" | Out-Null } else { Skip-Step -Point 'EVENT' -Case 'event-feedback' -Method 'POST' -Path '/api/event/{id}/feedback' -Reason 'eventId missing' }
if ($e1Id) { Invoke-Step -Point 'EVENT' -Case 'close-event' -Method 'POST' -Path "/api/event/$e1Id/close?closeUser=1&closeReason=$(Url-Encode 'auto-close')" | Out-Null } else { Skip-Step -Point 'EVENT' -Case 'close-event' -Method 'POST' -Path '/api/event/{id}/close' -Reason 'eventId missing' }

# 4) risk point + rules
$rpCode = "AUTO-RP-$stamp"
$rp = Invoke-Step -Point 'RISK-POINT' -Case 'add-risk-point' -Method 'POST' -Path '/api/risk-point/add' -Body @{
    riskPointCode     = $rpCode
    riskPointName     = 'auto-risk-point'
    regionId          = 1
    regionName        = 'default-region'
    responsibleUser   = 1
    responsiblePhone  = '13800000000'
    riskLevel         = 'warning'
    description       = 'auto-test'
    status            = 0
    tenantId          = 1
}
$rpId = Id-Of $rp
Invoke-Step -Point 'RISK-POINT' -Case 'list-risk-points' -Method 'GET' -Path "/api/risk-point/list?riskPointCode=$rpCode" | Out-Null
if ($rpId) {
    Invoke-Step -Point 'RISK-POINT' -Case 'get-risk-point' -Method 'GET' -Path "/api/risk-point/get/$rpId" | Out-Null
    Invoke-Step -Point 'RISK-POINT' -Case 'bind-device' -Method 'POST' -Path '/api/risk-point/bind-device' -Body @{
        riskPointId      = $rpId
        deviceId         = $deviceId
        deviceCode       = $deviceCode
        deviceName       = $deviceName
        metricIdentifier = 'temperature'
        metricName       = 'temperature'
        defaultThreshold = '80'
        thresholdUnit    = 'C'
    } | Out-Null
    Invoke-Step -Point 'RISK-POINT' -Case 'list-bound-devices' -Method 'GET' -Path "/api/risk-point/bound-devices/$rpId" | Out-Null
    Invoke-Step -Point 'RISK-POINT' -Case 'unbind-device' -Method 'POST' -Path "/api/risk-point/unbind-device?riskPointId=$rpId&deviceId=$deviceId" | Out-Null
    Invoke-Step -Point 'RISK-POINT' -Case 'update-risk-point' -Method 'POST' -Path '/api/risk-point/update' -Body @{
        id               = $rpId
        riskPointCode    = $rpCode
        riskPointName    = 'auto-risk-point-upd'
        regionId         = 1
        regionName       = 'default-region'
        responsibleUser  = 1
        responsiblePhone = '13800000000'
        riskLevel        = 'critical'
        description      = 'auto-update'
        status           = 0
        tenantId         = 1
    } | Out-Null
    Invoke-Step -Point 'RISK-POINT' -Case 'delete-risk-point' -Method 'POST' -Path "/api/risk-point/delete/$rpId" | Out-Null
} else {
    Skip-Step -Point 'RISK-POINT' -Case 'get-risk-point' -Method 'GET' -Path '/api/risk-point/get/{id}' -Reason 'riskPointId missing'
}

$ruleName = "AUTO-RULE-$stamp"
$rule = Invoke-Step -Point 'RULE-DEFINITION' -Case 'add-rule' -Method 'POST' -Path '/api/rule-definition/add' -Body @{
    ruleName            = $ruleName
    metricIdentifier    = 'temperature'
    metricName          = 'temperature'
    expression          = 'value > 80'
    duration            = 60
    alarmLevel          = 'warning'
    notificationMethods = 'email'
    convertToEvent      = 1
    status              = 0
    tenantId            = 1
}
$ruleId = Id-Of $rule
Invoke-Step -Point 'RULE-DEFINITION' -Case 'list-rules' -Method 'GET' -Path '/api/rule-definition/list?metricIdentifier=temperature' | Out-Null
if ($ruleId) {
    Invoke-Step -Point 'RULE-DEFINITION' -Case 'get-rule' -Method 'GET' -Path "/api/rule-definition/get/$ruleId" | Out-Null
    Invoke-Step -Point 'RULE-DEFINITION' -Case 'update-rule' -Method 'POST' -Path '/api/rule-definition/update' -Body @{
        id                 = $ruleId
        ruleName           = "$ruleName-upd"
        metricIdentifier   = 'temperature'
        metricName         = 'temperature'
        expression         = 'value > 85'
        duration           = 120
        alarmLevel         = 'critical'
        notificationMethods = 'email,sms'
        convertToEvent     = 1
        status             = 0
        tenantId           = 1
    } | Out-Null
    Invoke-Step -Point 'RULE-DEFINITION' -Case 'delete-rule' -Method 'POST' -Path "/api/rule-definition/delete/$ruleId" | Out-Null
} else {
    Skip-Step -Point 'RULE-DEFINITION' -Case 'get-rule' -Method 'GET' -Path '/api/rule-definition/get/{id}' -Reason 'ruleId missing'
}

$linkName = "AUTO-LINK-$stamp"
$link = Invoke-Step -Point 'LINKAGE-RULE' -Case 'add-linkage' -Method 'POST' -Path '/api/linkage-rule/add' -Body @{
    ruleName         = $linkName
    description      = 'auto-linkage'
    triggerCondition = '{"metric":"temperature","operator":">","value":80}'
    actionList       = '[{"type":"notify","channel":"email"}]'
    status           = 0
    tenantId         = 1
}
$linkId = Id-Of $link
Invoke-Step -Point 'LINKAGE-RULE' -Case 'list-linkage' -Method 'GET' -Path "/api/linkage-rule/list?ruleName=$linkName" | Out-Null
if ($linkId) {
    Invoke-Step -Point 'LINKAGE-RULE' -Case 'get-linkage' -Method 'GET' -Path "/api/linkage-rule/get/$linkId" | Out-Null
    Invoke-Step -Point 'LINKAGE-RULE' -Case 'update-linkage' -Method 'POST' -Path '/api/linkage-rule/update' -Body @{
        id               = $linkId
        ruleName         = "$linkName-upd"
        description      = 'auto-linkage-upd'
        triggerCondition = '{"metric":"temperature","operator":">","value":85}'
        actionList       = '[{"type":"notify","channel":"sms"}]'
        status           = 0
        tenantId         = 1
    } | Out-Null
    Invoke-Step -Point 'LINKAGE-RULE' -Case 'delete-linkage' -Method 'POST' -Path "/api/linkage-rule/delete/$linkId" | Out-Null
}

$planName = "AUTO-PLAN-$stamp"
$plan = Invoke-Step -Point 'EMERGENCY-PLAN' -Case 'add-plan' -Method 'POST' -Path '/api/emergency-plan/add' -Body @{
    planName      = $planName
    riskLevel     = 'warning'
    description   = 'auto-plan'
    responseSteps = '[{"step":1,"action":"check"}]'
    contactList   = '[{"name":"oncall","phone":"13800000000"}]'
    status        = 0
    tenantId      = 1
}
$planId = Id-Of $plan
Invoke-Step -Point 'EMERGENCY-PLAN' -Case 'list-plans' -Method 'GET' -Path "/api/emergency-plan/list?planName=$planName" | Out-Null
if ($planId) {
    Invoke-Step -Point 'EMERGENCY-PLAN' -Case 'get-plan' -Method 'GET' -Path "/api/emergency-plan/get/$planId" | Out-Null
    Invoke-Step -Point 'EMERGENCY-PLAN' -Case 'update-plan' -Method 'POST' -Path '/api/emergency-plan/update' -Body @{
        id            = $planId
        planName      = "$planName-upd"
        riskLevel     = 'critical'
        description   = 'auto-plan-upd'
        responseSteps = '[{"step":1,"action":"act-now"}]'
        contactList   = '[{"name":"owner","phone":"13900000000"}]'
        status        = 0
        tenantId      = 1
    } | Out-Null
    Invoke-Step -Point 'EMERGENCY-PLAN' -Case 'delete-plan' -Method 'POST' -Path "/api/emergency-plan/delete/$planId" | Out-Null
}

# 5) reports
$startDate = (Get-Date).AddDays(-7).ToString('yyyy-MM-dd')
$endDate = (Get-Date).ToString('yyyy-MM-dd')
Invoke-Step -Point 'REPORT' -Case 'risk-trend' -Method 'GET' -Path "/api/report/risk-trend?startDate=$startDate&endDate=$endDate" | Out-Null
Invoke-Step -Point 'REPORT' -Case 'alarm-stat' -Method 'GET' -Path "/api/report/alarm-statistics?startDate=$startDate&endDate=$endDate" | Out-Null
Invoke-Step -Point 'REPORT' -Case 'event-closure' -Method 'GET' -Path "/api/report/event-closure?startDate=$startDate&endDate=$endDate" | Out-Null
Invoke-Step -Point 'REPORT' -Case 'device-health' -Method 'GET' -Path '/api/report/device-health' | Out-Null

# 6) system management
$orgCode = "AUTO_ORG_$stamp"
$org = Invoke-Step -Point 'SYS-ORG' -Case 'add-org' -Method 'POST' -Path '/api/organization' -Body @{
    tenantId     = 1
    parentId     = 0
    orgName      = 'auto-org'
    orgCode      = $orgCode
    orgType      = 'dept'
    leaderUserId = 1
    leaderName   = 'admin'
    phone        = '13800000000'
    email        = 'auto-org@test.com'
    status       = 1
    sortNo       = 1
}
$orgId = Id-Of $org
Invoke-Step -Point 'SYS-ORG' -Case 'list-org' -Method 'GET' -Path '/api/organization/list' | Out-Null
Invoke-Step -Point 'SYS-ORG' -Case 'tree-org' -Method 'GET' -Path '/api/organization/tree' | Out-Null
if ($orgId) {
    Invoke-Step -Point 'SYS-ORG' -Case 'get-org' -Method 'GET' -Path "/api/organization/$orgId" | Out-Null
    Invoke-Step -Point 'SYS-ORG' -Case 'update-org' -Method 'PUT' -Path '/api/organization' -Body @{
        id           = $orgId
        tenantId     = 1
        parentId     = 0
        orgName      = 'auto-org-upd'
        orgCode      = $orgCode
        orgType      = 'dept'
        leaderUserId = 1
        leaderName   = 'admin'
        phone        = '13800000000'
        email        = 'auto-org@test.com'
        status       = 1
        sortNo       = 2
    } | Out-Null
    Invoke-Step -Point 'SYS-ORG' -Case 'delete-org' -Method 'DELETE' -Path "/api/organization/$orgId" | Out-Null
}

$username = "auto_user_$stamp"
Invoke-Step -Point 'SYS-USER' -Case 'add-user' -Method 'POST' -Path '/api/user/add' -Body @{
    tenantId = 1
    username = $username
    password = '123456'
    realName = 'auto-user'
    phone    = "1390000$($stamp.Substring($stamp.Length - 4))"
    email    = "auto.user.$stamp@test.com"
    createBy = 1
} | Out-Null
Invoke-Step -Point 'SYS-USER' -Case 'list-user' -Method 'GET' -Path "/api/user/list?username=$username" | Out-Null
$userByName = Invoke-Step -Point 'SYS-USER' -Case 'get-user-by-username' -Method 'GET' -Path "/api/user/username/$username"
$userId = $null
if ($userByName -and $userByName.data -and $userByName.data.id) { $userId = [long]$userByName.data.id }
if ($userId) {
    Invoke-Step -Point 'SYS-USER' -Case 'get-user' -Method 'GET' -Path "/api/user/$userId" | Out-Null
    Invoke-Step -Point 'SYS-USER' -Case 'update-user' -Method 'PUT' -Path '/api/user/update' -Body @{
        id       = $userId
        tenantId = 1
        username = $username
        realName = 'auto-user-upd'
        phone    = '13900000001'
        email    = "auto.user.$stamp@test.com"
        status   = 1
        updateBy = 1
    } | Out-Null
    Invoke-Step -Point 'SYS-USER' -Case 'reset-password' -Method 'POST' -Path "/api/user/reset-password/$userId" | Out-Null
    Invoke-Step -Point 'SYS-USER' -Case 'delete-user' -Method 'DELETE' -Path "/api/user/$userId" | Out-Null
}

$roleCode = "AUTO_ROLE_$stamp"
Invoke-Step -Point 'SYS-ROLE' -Case 'add-role' -Method 'POST' -Path '/api/role/add' -Body @{
    tenantId    = 1
    roleName    = 'auto-role'
    roleCode    = $roleCode
    description = 'auto-role-desc'
    status      = 1
    createBy    = 1
} | Out-Null
$roleList = Invoke-Step -Point 'SYS-ROLE' -Case 'list-role' -Method 'GET' -Path "/api/role/list?roleCode=$roleCode"
$roleId = $null
if ($roleList -and $roleList.data -and $roleList.data.Count -gt 0) { $roleId = [long]$roleList.data[0].id }
if ($roleId) {
    Invoke-Step -Point 'SYS-ROLE' -Case 'get-role' -Method 'GET' -Path "/api/role/$roleId" | Out-Null
    Invoke-Step -Point 'SYS-ROLE' -Case 'update-role' -Method 'PUT' -Path '/api/role/update' -Body @{
        id          = $roleId
        tenantId    = 1
        roleName    = 'auto-role-upd'
        roleCode    = $roleCode
        description = 'auto-role-desc-upd'
        status      = 1
        updateBy    = 1
    } | Out-Null
    if ($userId) { Invoke-Step -Point 'SYS-ROLE' -Case 'list-user-roles' -Method 'GET' -Path "/api/role/user/$userId" | Out-Null } else { Skip-Step -Point 'SYS-ROLE' -Case 'list-user-roles' -Method 'GET' -Path '/api/role/user/{userId}' -Reason 'userId missing' }
    Invoke-Step -Point 'SYS-ROLE' -Case 'delete-role' -Method 'DELETE' -Path "/api/role/$roleId" | Out-Null
}

$regionCode = "AUTO_REGION_$stamp"
$r = Invoke-Step -Point 'SYS-REGION' -Case 'add-region' -Method 'POST' -Path '/api/region' -Body @{
    tenantId   = 1
    regionName = 'auto-region'
    regionCode = $regionCode
    parentId   = 0
    regionType = 'city'
    status     = 1
    sortNo     = 1
    remark     = 'auto-region'
}
$regionId = Id-Of $r
Invoke-Step -Point 'SYS-REGION' -Case 'list-region' -Method 'GET' -Path '/api/region/list' | Out-Null
Invoke-Step -Point 'SYS-REGION' -Case 'tree-region' -Method 'GET' -Path '/api/region/tree' | Out-Null
if ($regionId) {
    Invoke-Step -Point 'SYS-REGION' -Case 'get-region' -Method 'GET' -Path "/api/region/$regionId" | Out-Null
    Invoke-Step -Point 'SYS-REGION' -Case 'update-region' -Method 'PUT' -Path '/api/region' -Body @{
        id         = $regionId
        tenantId   = 1
        regionName = 'auto-region-upd'
        regionCode = $regionCode
        parentId   = 0
        regionType = 'city'
        status     = 1
        sortNo     = 2
        remark     = 'auto-region-upd'
    } | Out-Null
    Invoke-Step -Point 'SYS-REGION' -Case 'delete-region' -Method 'DELETE' -Path "/api/region/$regionId" | Out-Null
}

$dictCode = "AUTO_DICT_$stamp"
$dict = Invoke-Step -Point 'SYS-DICT' -Case 'add-dict' -Method 'POST' -Path '/api/dict' -Body @{
    tenantId = 1
    dictName = 'auto-dict'
    dictCode = $dictCode
    dictType = 'text'
    status   = 1
    sortNo   = 1
    remark   = 'auto-dict'
}
$dictId = Id-Of $dict
Invoke-Step -Point 'SYS-DICT' -Case 'list-dict' -Method 'GET' -Path '/api/dict/list' | Out-Null
Invoke-Step -Point 'SYS-DICT' -Case 'tree-dict' -Method 'GET' -Path '/api/dict/tree' | Out-Null
if ($dictId) { Invoke-Step -Point 'SYS-DICT' -Case 'get-dict' -Method 'GET' -Path "/api/dict/$dictId" | Out-Null } else { Skip-Step -Point 'SYS-DICT' -Case 'get-dict' -Method 'GET' -Path '/api/dict/{id}' -Reason 'dictId missing' }
Invoke-Step -Point 'SYS-DICT' -Case 'get-dict-by-code' -Method 'GET' -Path "/api/dict/code/$dictCode" | Out-Null
if ($dictId) {
    Invoke-Step -Point 'SYS-DICT' -Case 'update-dict' -Method 'PUT' -Path '/api/dict' -Body @{
        id       = $dictId
        tenantId = 1
        dictName = 'auto-dict-upd'
        dictCode = $dictCode
        dictType = 'text'
        status   = 1
        sortNo   = 2
        remark   = 'auto-dict-upd'
    } | Out-Null
    Invoke-Step -Point 'SYS-DICT' -Case 'delete-dict' -Method 'DELETE' -Path "/api/dict/$dictId" | Out-Null
}

$channelCode = "auto_channel_$stamp"
$channel = Invoke-Step -Point 'SYS-CHANNEL' -Case 'add-channel' -Method 'POST' -Path '/api/system/channel/add' -Body @{
    tenantId    = 1
    channelName = 'auto-channel'
    channelCode = $channelCode
    channelType = 'email'
    config      = '{"host":"smtp.example.com"}'
    status      = 1
    sortNo      = 1
}
$channelId = Id-Of $channel
Invoke-Step -Point 'SYS-CHANNEL' -Case 'list-channel' -Method 'GET' -Path '/api/system/channel/list' | Out-Null
Invoke-Step -Point 'SYS-CHANNEL' -Case 'get-channel-by-code' -Method 'GET' -Path "/api/system/channel/getByCode/$channelCode" | Out-Null
if ($channelId) {
    Invoke-Step -Point 'SYS-CHANNEL' -Case 'update-channel' -Method 'PUT' -Path '/api/system/channel/update' -Body @{
        id          = $channelId
        tenantId    = 1
        channelName = 'auto-channel-upd'
        channelCode = $channelCode
        channelType = 'email'
        config      = '{"host":"smtp.example.com","port":25}'
        status      = 1
        sortNo      = 2
    } | Out-Null
    Invoke-Step -Point 'SYS-CHANNEL' -Case 'delete-channel' -Method 'DELETE' -Path "/api/system/channel/delete/$channelId" | Out-Null
}

Invoke-Step -Point 'SYS-AUDIT' -Case 'add-audit' -Method 'POST' -Path '/api/system/audit-log/add' -Body @{
    tenantId        = 1
    userId          = 1
    userName        = 'auto'
    operationType   = 'crud'
    operationModule = 'automation'
    operationMethod = 'runBusinessSmoke'
    requestUrl      = '/automation/smoke'
    requestMethod   = 'POST'
    requestParams   = '{}'
    responseResult  = '{}'
    ipAddress       = '127.0.0.1'
    location        = 'local'
    operationResult = 1
    resultMessage   = 'ok'
    operationTime   = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
} | Out-Null
$auditList = Invoke-Step -Point 'SYS-AUDIT' -Case 'list-audit' -Method 'GET' -Path '/api/system/audit-log/list?operationModule=automation'
Invoke-Step -Point 'SYS-AUDIT' -Case 'page-audit' -Method 'GET' -Path '/api/system/audit-log/page?pageNum=1&pageSize=10' | Out-Null
$auditId = $null
if ($auditList -and $auditList.data -and $auditList.data.Count -gt 0) { $auditId = [long]$auditList.data[0].id }
if ($auditId) {
    Invoke-Step -Point 'SYS-AUDIT' -Case 'get-audit' -Method 'GET' -Path "/api/system/audit-log/get/$auditId" | Out-Null
    Invoke-Step -Point 'SYS-AUDIT' -Case 'delete-audit' -Method 'DELETE' -Path "/api/system/audit-log/delete/$auditId" | Out-Null
}

# summary
$summary = $results | Group-Object point | ForEach-Object {
    $critical = @($_.Group | Where-Object { $_.critical -eq $true })
    if ($critical.Count -eq 0) { $critical = @($_.Group) }
    $pass = @($critical | Where-Object { $_.status -eq 'PASS' }).Count
    $total = [int]$critical.Count
    [pscustomobject]@{
        point        = $_.Name
        criticalPass = $pass
        criticalTotal = $total
        status       = $(if ($total -gt 0 -and $pass -eq $total) { 'PASS' } else { 'FAIL' })
    }
}

$jsonPath = Join-Path $outDir "business-function-smoke-$stamp.json"
$summaryPath = Join-Path $outDir "business-function-summary-$stamp.json"
$mdPath = Join-Path $outDir "business-function-report-$stamp.md"

$results | ConvertTo-Json -Depth 8 | Set-Content -Path $jsonPath -Encoding utf8
$summary | ConvertTo-Json -Depth 4 | Set-Content -Path $summaryPath -Encoding utf8

$md = New-Object System.Text.StringBuilder
[void]$md.AppendLine('# Business Function Smoke Report')
[void]$md.AppendLine('')
[void]$md.AppendLine("- Run time: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss'))")
[void]$md.AppendLine("- Base URL: $baseUrl")
[void]$md.AppendLine("- Baseline doc: docs/21-业务功能清单与验收标准.md")
[void]$md.AppendLine('')
[void]$md.AppendLine('## Function Point Summary')
[void]$md.AppendLine('')
[void]$md.AppendLine('| Function Point | Critical Passed | Status |')
[void]$md.AppendLine('|---|---:|---|')
foreach ($s in $summary | Sort-Object point) {
    [void]$md.AppendLine("| $($s.point) | $($s.criticalPass)/$($s.criticalTotal) | $($s.status) |")
}
[void]$md.AppendLine('')
[void]$md.AppendLine('## Failed Cases')
$failed = $results | Where-Object { $_.status -ne 'PASS' }
if ($failed.Count -eq 0) {
    [void]$md.AppendLine('- All cases passed.')
} else {
    foreach ($f in $failed) {
        [void]$md.AppendLine("- [$($f.point)] $($f.case) ($($f.method) $($f.path)): $($f.detail)")
    }
}
$md.ToString() | Set-Content -Path $mdPath -Encoding utf8

Write-Output "REPORT_JSON=$jsonPath"
Write-Output "REPORT_SUMMARY=$summaryPath"
Write-Output "REPORT_MD=$mdPath"
Write-Output "TOTAL_CASES=$($results.Count)"
Write-Output "TOTAL_FAILED=$($failed.Count)"
