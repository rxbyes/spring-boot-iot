# spring-boot-iot-ui 优化方案

## 2026-03-21 设备资产结果区去冗余卡片

- 调整页面：`/devices`。
- 本轮落地内容：
  - 移除设备页外层容器（`device-asset-view`）的边框与渐变背景，避免在主卡片外再出现一层大圆角卡片外壳。
  - 移除设备资产中心结果区容器（`device-result-panel`）的渐变背景和圆角卡片底板，仅保留列表本体，避免在主卡片内再出现一层视觉卡片包裹。
  - 结果区加载蒙层改为轻量透明遮罩，不再叠加模糊底板，减少“表格下再套卡片”的层次感。
- 防回退要求：
  - 单主卡台账页默认保持“无外层壳卡 + 无内层结果卡”口径；若确需重新引入额外卡片层，必须有明确业务信息分组价值。

## 2026-03-20 产品/设备列表样式纠偏（按钮对比度与操作收口）

- 调整页面：`/products`、`/devices`。
- 本轮落地内容：
  - 修复查询行主操作按钮文字可读性：`新增产品`、`新增设备` 与 `查询` 按钮统一使用主色实心样式，避免 `primary plain` 在当前主题下出现低对比度文案。
  - 设备页筛选区改为“字段区 + 动作区”两段结构，动作区统一承载 `查询 / 重置 / 新增设备 / 批量导入 / 展开全部筛选项(收起)`，展开与收起交互更接近阿里云台账筛选节奏。
  - 设备桌面表格移除“父子关系”列，避免在主表过早堆叠拓扑细节；父设备/网关信息继续保留在详情与维护抽屉中查看。
  - 设备桌面表格“操作”列由平铺 5 个动作收口为“详情 + 编辑 + 更多下拉”，与产品定义中心保持同口径。
  - 设备页补充桌面断点兜底（`>=721px` 强制表格视图，移动卡片仅窄屏启用），避免出现“表格下再套一层卡片”的重复展示感。
- 本轮新增防回退规则：
  - 查询行动作区的主入口按钮必须保持可读性优先，不得为了弱化视觉而牺牲按钮文本对比度。
  - 台账表格“操作”列新增动作时，默认先评估是否进入“更多”下拉，不再回退到横向平铺多按钮。

## 2026-03-20 链路验证中心明文/密文模拟改造

- 目标页面：`/reporting`（`ReportWorkbenchView.vue`）。
- 本轮落地内容：
  - 新增“明文上报 / 密文上报”模式切换，统一在链路验证中心完成双模式联调。
  - 明文模式按自然灾害技术标准自动识别 C.1/C.2/C.3：
    - C.3：命中 `did/ds_id/at/desc` 文件描述字段。
    - C.2：命中“时间戳 -> 值/对象”结构。
    - 其他：默认按 C.1。
  - 明文模式自动计算 Byte2~Byte3 大端长度（第 4 字节开始），并构造标准二进制帧预览（十进制/十六进制）。
  - C.3 额外支持文件流 Base64 输入，自动计算 `n+1~n+2` 文件流长度。
  - 密文模式不再做类型与长度计算，直接透传 `header + bodies.body` 封包。
  - HTTP 上报请求新增可选 `payloadEncoding`，明文帧发送时固定使用 `ISO-8859-1`，确保浏览器字符串可无损恢复 0~255 原始字节。
  - 页面进一步收口为“双栏工作台”最小修复版：左栏固定为输入与发送，右栏固定为诊断摘要、折叠详情和请求响应，减少输入区与调试信息混排导致的凌乱感。
  - 模板区按上报模式过滤：明文模式仅展示明文模板，密文模式仅展示密文模板。
  - 发送前新增阻断校验：必填项、明文 JSON 可解析、密文封包结构完整、C.3 Base64 合法；校验不通过时禁用发送按钮。
  - 错误提示分层：输入校验错误仅在左栏输入区展示，请求失败仅在右栏响应区展示，避免错误来源混淆。
  - 诊断信息分层折叠：默认只展示模式/识别类型/长度/推荐 Topic/可发送状态，十进制帧、十六进制帧、归一化 JSON、curl 预览改为按需展开。
- 新增防回退规则：
  - 链路验证中心后续新增协议实验能力时，明文帧头（类型 + 大端长度）和密文透传模式必须同时保留，不得回退到单一“纯 JSON 文本上报”。
  - 页面上报预览必须给出可核对的类型判定依据和长度结果，避免只给“发送成功/失败”而无法排查协议格式。
  - 链路验证中心必须保持“发送前校验阻断 + 发送后响应复盘”的分区策略，不得回退为单一通用错误提示。
  - 链路诊断详情默认折叠；后续新增诊断项优先进入折叠区，避免持续抬高首屏信息密度。

## 2026-03-20 产品/设备查询行统一收口

- 推广页面：`/products`、`/devices`。
- 本轮落地内容：
  - 产品定义中心已把“新增产品”从页面主标题右侧迁回筛选行，当前顺序为 `查询 -> 重置 -> 新增产品`，保持主操作与筛选动作同层。
  - 设备资产中心已把“新增设备 / 批量导入”并入筛选行，当前顺序为 `查询 -> 重置 -> 新增设备 -> 批量导入`。
  - 设备资产中心筛选项超过 3 个后改为自动收缩结构：默认显示 `设备编码 / 设备名称 / 产品 Key`，其余 `设备 ID / 在线状态 / 激活状态 / 设备状态` 收纳到“更多条件”折叠区，支持展开与收起。
  - 设备页筛选区、统计栏、结果区继续与产品页保持同一主卡样式口径，后续列表页默认按该模板扩展；如需跨模块复用，优先抽离为公共筛选头组件。
- 本轮新增防回退规则：
  - 列表页筛选项超过 3 个时，默认启用“常用条件 + 更多条件”折叠结构，不再把全部筛选项平铺在首行。
  - 若页面主操作直接服务当前列表查询，应优先并入查询行；同一操作不得在主标题区和查询行重复出现。
  - 当该模板在 3 个及以上列表页重复落地时，统一抽离公共查询头组件（暂定 `ListFilterHeader`）；在达到阈值前，先按产品页模板保持结构一致，避免过早抽象。

## 2026-03-20 设备页列表体验推广

- 推广页面：`/devices`（`DeviceWorkbenchView`）。
- 本轮落地内容：
  - 设备列表已补齐与 `/products` 同口径的分页结果缓存、`sessionStorage` 会话恢复、静默校验和下一页预取能力。
  - 设备列表已补齐路由 query 恢复，当前覆盖 `deviceId / productKey / deviceCode / deviceName / onlineStatus / activateStatus / deviceStatus / pageNum / pageSize`。
  - 设备列表请求与详情请求已接入 `AbortController`，快速切筛选、翻页、刷新和重复打开详情时不再让旧请求覆盖新结果。
  - 设备结果区已补齐冷启动骨架屏、浅色局部蒙层和页内静默刷新提示，不再直接对整表使用深色 `v-loading` 遮罩。
  - 设备列表空状态已拆分为“尚无设备资产”和“筛选无结果”两类，并补齐对应下一步动作。
  - 设备页首屏已移除产品选项预加载；新增、编辑、设备更换抽屉改为首次打开时再懒加载产品下拉数据。
  - 设备页本轮同步完成了 eyebrow 中文化、结果卡描述去总数化，避免与分页总数重复表达。
  - 设备列表已补齐窄屏卡片模式：桌面端继续保留表格，窄屏切换为设备卡片列表，并保留 `详情 / 编辑 / 更多` 操作入口。
  - 设备列表的桌面表格与移动卡片已统一复用同一份选中态和批量操作状态；卡片勾选会同步回桌面表格选中结果，避免视图切换后批量行为割裂。
  - 设备详情抽屉已接入“列表摘要秒开 + 完整详情后台补数 + 本地详情缓存”闭环；命中缓存或同一对象重复打开时优先复用最近详情，跨路由返回后仍可从 `sessionStorage` 恢复最近详情快照。
  - 设备编辑抽屉已补齐“列表摘要或本地缓存预填 + 后台补全最新档案 + 脏表单保护”闭环；首次打开新增/编辑时不再等待产品下拉和完整详情都返回，产品下拉请求也会做同轮去重。
  - 设备新增/编辑提交成功后已补齐“当前页结果本地回写 + 后台静默校验”闭环；命中过滤条件时会优先同步当前可见行、分页总数和选中态，再后台校正分页与排序，减少抽屉关闭后列表短暂停留旧值的割裂感。
  - 设备删除、批量删除和设备更换成功后也已补齐结果区即时反馈：删除类操作会先本地移除当前页可见行并同步总数，设备更换会先按已知语义更新旧设备停用态、生成新设备摘要快照并秒开详情，再后台校正最终分页与排序。
  - 设备更换抽屉已补齐“摘要预填秒开 + 后台补全最新档案 + 脏表单保护”模式，替换场景不再阻塞等待完整详情返回后才打开。
  - 设备页已新增父子拓扑治理试点：新增、编辑、设备更换、批量导入、详情和列表展示已统一补齐父设备/网关信息入口，后续其他带拓扑关系的资产页必须同步遵守“展示入口和修改入口成对出现”的治理规则。
- 本轮判断后的布局结论：
  - 设备页已与 `/products` 对齐为单主卡紧凑结构，页面说明、筛选条、统计栏和结果区继续同卡承接。
  - 设备页筛选当前收口为“默认 3 项 + 更多条件 4 项”：默认显示 `设备编码 / 设备名称 / 产品 Key`，折叠区承接 `设备 ID / 在线状态 / 激活状态 / 设备状态`。
- 下一步建议：
  - 后续若新增筛选项超过当前密度，先做“是否高频”评估；非高频项优先通过路由 query 或联动页承接，避免回退到“折叠筛选 + 提示条”双层结构。

## 2026-03-19 产品页二次收口试点

- 试点页面：`/products`（`ProductWorkbenchView`）。
- 本轮收口结论：
  - 当列表页最终只保留 3 个以内的高频筛选条件时，不再保留“常用条件 + 更多条件”的展开骨架，也不再额外展示“已展开全部筛选项”“常用条件说明”这类解释性文案；筛选项与查询/重置按钮应直接同列展示，优先保证首屏紧凑度和操作直达性。
  - 当页面定位说明、主操作、筛选条、统计栏和台账列表属于同一业务闭环时，应优先收口为单卡结构，不再拆成上下两张风格接近的卡片；普通产品定义页不应再为了保留标题说明额外多出一层外壳。
  - 当台账卡头已存在稳定标题、主按钮和统计栏时，筛选条应优先内嵌在“标题区”和“统计栏”之间，而不是继续单独占用一张筛选卡；移动后字段不再展示独立 label，只保留 placeholder，进一步压缩纵向空间。
  - 筛选输入在桌面端可以按主次比例分栏，但不得依赖固定宽度；应通过响应式 grid 保留字段间距，并在平板/手机端自然降为两列或一列，避免窄屏出现横向挤压或截断。
  - 已执行查询的筛选条件必须有显式反馈，优先在工具栏前增加“已生效筛选”标签行，并支持单项关闭和清空全部；筛选标签只反映最近一次真实请求，不跟随用户未提交的临时输入抖动。
  - 列表总量若已由 `StandardPagination` 清晰表达，页面首屏和工具栏中不应再重复渲染“当前结果 xx 条”类统计文案，避免同一信息重复占位。
  - 如果 Hero 区展示的启用/停用指标与台账统计栏完全同义，应直接删除 Hero 重复指标；普通产品定义页不应再额外挂载“在线设备数”这类偏资产汇总口径。
  - 空状态必须和业务语义绑定：未建档时应引导新增产品，筛选无结果时应提示调整或清空条件；移动端卡片列表和桌面端表格不得各自维护一套割裂的空态文案。
  - 卡片内列表查询/刷新属于弱打断加载，不应直接复用全局深色 `mask`；结果区应使用浅色局部蒙层、保持原有圆角和底板，避免局部刷新时出现整块黑屏。
  - 高频台账页在冷启动或未命中缓存时，结果区应优先展示与最终列表结构接近的骨架屏，避免只留白底容器和 spinner 造成页面主结构短暂塌空。
  - 当普通产品台账页主动移除台账卡标题、只保留一个全局主操作时，主按钮应锚定在页面主标题右侧；同一主操作不再在 Hero 与台账卡头重复出现，避免首屏出现两个视觉重心。
  - “库存”语义只允许出现在真实库存管理页面；产品定义、设备接入、关联设备跳转等页面若并不承担库存管理职责，必须改回“关联设备”“在线设备”“设备数量”等真实业务表达，避免领域概念误导。
  - 列表、详情、导出预设和说明文案需要同步收口；不能只删表格列，仍在详情抽屉、导出列设置或维护规则中保留旧口径。
  - 产品、设备等资产台账详情抽屉必须保持“概览 Hero + 分组字段卡 + 治理提示”的统一结构，不得退回为无分区、无卡片层次的纯文本纵向堆叠。
  - 台账详情抽屉首屏应保留一个明确的“主信息焦点区”，用于突出当前状态、治理阶段或核心身份；其余摘要指标作为次级信息展开，避免所有卡片平均分配视觉权重。
  - 同一属性应在详情抽屉内单点展示，尤其是 `状态 / 节点类型 / 协议编码 / 数据格式 / 时间` 这类核心字段，不得在标题 tags、Hero、摘要卡和字段卡中重复刷屏。
  - 产品定义中心详情必须把“产品汇总数据”“接入契约信息”“产品档案信息”“维护治理建议”分区展示，避免把设备规模、接入语义、建档档案混在同一块里。
  - 面向业务侧的详情抽屉文案应优先保持中文一致性，并尽量使用通俗业务语言，避免无意义的英文 eyebrow、section pill、状态标签或偏研发黑话打断阅读节奏。
  - 接入契约区优先采用单列纵向卡片，便于逐项核对协议、节点角色和数据格式；长编号和长 Key 应交给更宽的产品档案区承接，并提供 hover 查看完整值。
  - 维护与治理区应进一步拆成“当前建议 / 固定规则 / 变更前确认”等层次化卡片，禁止继续把多类治理文案塞进两块笼统的大段提示里。
  - 详情抽屉同层级卡片应统一字号、行距、内边距和区块间距，不要因为局部标题过大、说明过长或空白过多再次回到“结构对了但观感仍散”的状态。
  - 列表页若需支持刷新、回退或跨页面返回后的台账恢复，应把高频筛选项和分页状态同步到路由 query，不要只把状态留在组件内。
  - 列表页在快速切筛选、分页、刷新或路由恢复时，应主动取消过期请求，避免旧响应回写覆盖新结果。
  - 列表页的新增、编辑、删除提交成功后，应优先本地更新当前页可见结果，再后台静默刷新校正排序和分页，不要每次都阻塞等待整页重刷后才反馈结果。
  - 若列表页已接入分页缓存，增删改成功后的“后台静默刷新”不得被同一时刻刚写入的新鲜缓存短路；需要保留当前页本地结果秒开，但必须显式继续发起一次静默校验请求。
  - 当列表页已展示可用结果，只是在后台静默校验最新数据时，应优先使用结果区上方的轻量页内提示；静默校验失败时默认继续保留当前结果，并改为页内错误提示，而不是直接弹出打断式 toast。
  - 若详情抽屉同层级卡片仍出现“高的高、短的短”的失衡感，应优先统一 label / value / helper 的字号梯度，并把长说明拆到独立宽卡承接，不要继续把长文塞在档案卡里把整行节奏撑散。
  - 若详情抽屉中段仍出现“左列契约卡 / 右列档案卡”高度节奏不一致的问题，应优先检查共享 `info-chip` 基线是否带入额外 padding、label margin 和 stretch 拉伸；商业化收口阶段必须主动覆盖这些冗余留白。
  - 详情抽屉若接口补数慢于列表摘要，应优先使用列表行已有字段秒开，再异步补齐完整详情，并用轻量提示说明补数状态，不要一律清空抽屉等待整屏 loading。
  - 同一详情抽屉若频繁重复打开相同对象，应优先按 `id + updateTime` 复用本地缓存的完整详情；只有列表行版本时间变化或缓存信息不足时才重新请求。
  - 若详情/编辑抽屉存在跨路由返回后继续重复打开同一对象的链路，完整详情缓存还应具备短时会话级持久化能力；不要让页面一卸载就退回“同一对象重复补数”的旧体验。
  - 编辑抽屉若需要秒开，应优先使用列表摘要或本地缓存预填表单，再后台补齐最新详情；若用户已经开始编辑，后台返回的新数据不得自动覆盖当前输入。
  - 当用户明确反馈“仍然难看、同质化”时，不应继续在原模板上微调；应直接切换为另一套版式语言（例如三段式分区大板块），并通过主次版面和差异化底纹建立阅读层级。
  - 列表页展示关联设备数、在线数、最近上报等聚合字段时，后端应直接走数据库聚合返回结果；不要在 10 条分页场景下仍把当前页产品下全部设备明细拉回应用层再汇总。
- 本轮落地结果：
  - `ProductWorkbenchView` 已移除 Hero 区重复启用/停用/在线指标、展开筛选说明和全部库存口径。
  - 页面说明、主操作、筛选区、统计栏和表格已整合到同一张产品定义主卡片中，不再拆成上下两张卡片。
  - 页面筛选已收口为“产品名称 / 节点类型 / 产品状态”三项同列查询，并直接内嵌到主卡片说明区与统计栏之间；桌面端保持比例分栏，窄屏自动降列。
  - “新增产品”已迁移到页面主标题右侧；产品台账次级标题已移除，首屏只保留简洁定位说明，不再叠加同义统计卡和独立筛选卡。
  - 工具栏汇总已进一步移除“在线设备”口径，只保留 `已选 / 启用 / 停用` 三项，避免产品定义页再次混入资产在线统计。
  - 产品页已新增“已生效筛选”标签行，支持单项移除和清空全部；标签状态与最近一次已执行查询保持一致，不受用户未提交输入影响。
  - 数据列表操作列已压缩空白宽度，并同步收紧按钮间距，避免“详情 / 编辑 / 查看设备 / 删除”后方保留大段无效留白。
  - 产品列表已补齐移动端卡片模式：桌面端继续使用表格，窄屏切换为产品卡片列表，并保留勾选、详情、编辑和更多操作入口。
  - 产品列表区已收口为单一结果面板，统一承接加载态与空态；当前会区分“还没有产品定义”和“没有符合条件的产品”两类空状态，分别提供新增产品或清空筛选的下一步动作。
  - 产品页查询/刷新时的结果区蒙层已改为浅色局部加载态，并保留圆角与底板，不再沿用全局深色遮罩造成短暂黑屏。
  - 产品详情抽屉已补齐统一 eyebrow/subtitle、产品身份 Hero、摘要指标卡、分组字段卡和治理提醒，与设备资产中心详情抽屉保持同一视觉层次。
  - 产品详情抽屉首屏已进一步增加治理焦点卡、在线占比/契约阶段/维护时点摘要和分区识别 pill，让阅读顺序从“先看状态与阶段，再看字段细节”更自然。
  - 产品详情抽屉现已继续去重：标题区不再重复挂载状态/协议/节点类型标签，设备规模仅保留在“产品汇总”，协议与节点角色只保留在“接入契约”，ID / Product Key / 厂商 / 建档时间只保留在“产品档案”。
  - 产品详情抽屉已继续压缩说明文案、移除无必要英文标签，并整体收紧间距与卡片内边距，减少空白对阅读节奏的干扰。
  - 产品详情抽屉现已继续把“接入契约”收口为单列纵向卡片，“产品档案”改为更宽的长值档案区，并把“维护与治理”拆为“当前建议 / 维护规则 / 变更前确认”三类卡片；创建时间已收口到年月日展示。
  - 产品详情抽屉本轮继续完成文字与节奏细修：副标题、区块说明、治理提示已改为更通俗的业务表达，汇总卡、契约卡、档案卡与治理卡的字号、行高、留白和说明密度已进一步拉齐。
  - 产品详情抽屉本轮继续完成商业化版式收口：抽屉页眉、首个区块、区块之间、卡片之间的纵向节奏已再次压缩；接入契约区对共享 `info-chip` 的额外高度做了页面内覆盖，中段两列容器也补齐 `align-items: start`，卡片观感更整齐。
  - 产品详情抽屉本轮继续完成风格重构：从“同质卡片连续堆叠”切换为“汇总总览区 / 契约档案区 / 治理区”三段式版面，汇总区、档案区、治理区分别使用差异化底纹和卡片权重，整体观感由工具页转向商业化工作台。
  - 产品详情抽屉本轮继续把“补充说明”从档案卡中拆成独立宽卡，并统一契约卡、档案卡、治理卡的字号梯度、最小高度和区块间距，减少同一行卡片高低不齐的问题。
  - 产品筛选条件与分页状态已同步写回路由 query，刷新、回退或从其他页面返回后，可继续恢复当前台账视图。
  - 产品列表在快速切换筛选、分页、刷新或路由恢复时，已主动取消过期分页请求，避免旧响应回写覆盖当前结果。
  - 产品新增、编辑、删除成功后，当前页会先做本地结果更新，再后台静默刷新校正分页与排序，减少等待整页重刷的停顿感。
  - 产品页已继续修正“静默刷新被新鲜分页缓存短路”的问题；增删改成功后仍先复用当前页本地结果，但后台一定会继续发起一次静默校验请求，避免排序、分页和聚合字段校正被跳过。
  - 产品结果区已补齐冷启动骨架屏；未命中缓存时不再只显示空白蒙层，而是先稳定展示接近最终结构的桌面表格骨架和移动卡片骨架，减少首屏跳动感。
  - 产品列表在已有结果时的后台静默校验，已改为结果区上方的轻提示条；静默校验失败默认保留当前结果，并以内联错误提示替代打断式 toast，减少操作干扰。
  - 产品详情抽屉已支持“列表摘要秒开 + 完整详情补数”交互，减少整屏空白等待。
  - 产品详情抽屉已补齐本地详情缓存；同一产品在 `updateTime` 未变化时重复打开，将直接复用已缓存的完整详情，不再重复请求。
  - 产品详情缓存已继续下沉到 `sessionStorage` 会话级持久化；从设备页、详情链路或其他工作台返回 `/products` 后，再次打开同一产品的详情或编辑仍可先复用最近完整档案，再后台静默补数。
  - 产品编辑抽屉已支持“摘要预填 + 后台补数”秒开模式；若用户已开始编辑，后台补回的最新档案不会自动覆盖当前表单。
  - 产品列表已补齐“分页结果缓存 + 静默校验 + 下一页预取”试点能力；命中相同 `productName / nodeType / status / pageNum / pageSize` 时优先复用当前页结果，刷新或增删改成功后再失效并重建当前页缓存。
  - 产品列表分页缓存已继续下沉到 `sessionStorage` 会话级持久化；从详情页、设备页或其他工作台返回 `/products` 时，组件重新挂载仍会先恢复最近缓存，再后台静默校验。
  - 产品分页与详情中的关联设备统计已改为数据库聚合查询；默认 10 条分页不再把当前页产品下全部设备明细加载到应用层再汇总。
  - `iot_device` 已补充 `idx_device_deleted_product_stats` 聚合索引，用于支撑产品统计查询的真实环境性能。

## 一、现状分析

### 1.1 项目定位
spring-boot-iot-ui 是一个基于 Vue 3 + Element Plus + ECharts 的前端工作区，承接监测预警平台首页、真实业务页面与实施联调能力，服务于 spring-boot-iot 物联网平台。

### 1.2 当前实现状态

#### 已完成能力
- **页面结构**：当前已形成“平台首页 + 接入智维 / 风险运营 / 风险策略 / 平台治理 / 质量工场”五个一级工作台；`future-lab` 路由保留但已移出主导航
- **API对接**：产品、设备、HTTP上报、属性查询、消息日志等接口
- **组件体系**：PanelCard、MetricCard、ResponsePanel、PropertyTrendPanel等
- **状态管理**：tabs、activity、theme等store
- **权限架构**：登录态与按钮权限继续基于 `authContext + sys_menu/sys_role_menu/sys_user_role`，壳层一级/二级导航优先由后端菜单树动态驱动；仅在 `authContext.menus=[]` 的异常场景下才启用前端临时兜底分组
- **视觉设计**：公共壳层已升级为浅色产品控制台风格（顶部双层导航 + 左侧菜单），并已完成核心工作页与风险监测增强页（实时监测/GIS/详情抽屉）的浅色面板和图表主题统一；品牌统一为 `监测预警平台`
- **图表能力**：ECharts属性趋势图、实时数据展示
- **自动化测试中心**：新增 `/automation-test` 配置驱动测试编排页，支持在前端维护步骤、接口断言、变量捕获并导出浏览器巡检 JSON 计划；第二阶段已补齐页面盘点、覆盖分析、一键脚手架生成与手工补录外部页面能力；第三阶段已补齐插件式步骤注册与复杂动作配置（勾选、文件上传、表格行操作、弹窗操作）；第四阶段已补齐截图基线、视觉回归、diff 图片索引页、失败截图明细页与批量基线治理命令，前端已支持配置 `baselineDir`、`assertScreenshot`、`screenshotTarget`、`baselineName`、`threshold`、`fullPage`，并可配合 `npm run acceptance:browser:update-baseline`、`npm run acceptance:browser:baseline:manage` 完成基线刷新与治理
- **构建分包**：`vite.config.ts` 已接入 `unplugin-vue-components` 做 Element Plus 组件与指令按需导入，公共入口不再做全局注册；在此基础上，高频共享 UI 依赖已稳定收敛为 `vendor-element-core`、`vendor-element-form`、`vendor-element-table`、`vendor-element-panel` 四组，并保留 `vendor-vue` 等公共块；2026-03-17 针对 ECharts 分块进一步优化为 `vendor-echarts-core + vendor-zrender`，在保持按需引入的同时消除了大块与 empty chunk 告警

#### 已落地的产品化改造（2026-03）
- **品牌与定位**：首页、壳层与登录页统一切换为“监测预警平台”，不再使用 “Spring IOT 控制台” 等研发控制台表述
- **信息架构**：公共壳层已移除“快捷入口 / 常用入口 / 代理模式 / 费用与成本”等与当前业务定位不符的表达
- **首页重构**：首页已按 Hero、KPI、角色视角、能力构成、能力进展、风险处置闭环、经营主线入口、治理能力、待办中心重组
- **去工作区化**：平台首页已隐藏标签工作区与重复型重工具栏，改为轻量状态条，减少后台管理页观感
- **接入区命名**：`接入回放台`、`风险点工作台`、`文件与固件校验` 已进一步收口为 `链路验证中心`、`对象洞察台`、`数据校验台`
- **角色视角**：首页已加入 `值班人员 / 运维主管 / 管理层` 三种 preset，首屏内容不再是固定模板
- **待办中心**：右侧区域由“最近操作动态”改为角色化待办，本地 activity 只保留最近处理痕迹
- **入口分层**：核心入口收敛为五条业务主线，`设备接入与验证` 下调为实施支撑能力
- **数据策略**：首页首屏采用“静态能力编排 + activity store 动态兜底”，避免未稳定真实接口影响商业化首屏体验
- **导航策略**：`future-lab` 仍保留规划展示路由，但不再作为主导航核心入口
- **导航策略**：`AppShell` 一级/二级导航由 MySQL 菜单树动态构建，并统一复用命令面板触发器、分区条间距和角色化默认落点；身份区按当前登录用户与角色动态展示，按钮权限与角色授权仍来自 MySQL
- **头部体验**：顶部右侧补齐控制台工具区（消息/帮助入口），头像区展示当前登录账号与角色，增强全局信息可见性
- **头部体验**：顶部工具入口已统一为文字按钮（消息通知、帮助中心），取消圆形图标入口
- **头部交互**：消息通知与帮助中心已接入下拉面板并提供常用页面快捷跳转
- **交互可用性**：消息/帮助下拉面板支持点击空白区关闭与 `Esc` 关闭，减少误停留
- **无障碍增强**：头部工具按钮已补齐 `aria-expanded`、`aria-controls`，并在面板打开后自动聚焦首个可操作项
- **组件拆分**：`AppShell` 头部逻辑已拆分为 `AppHeaderTools.vue` 与 `HeaderPopoverPanel.vue`，降低壳层维护复杂度
- **壳层继续瘦身**：2026-03-20 起 `AppShell` 再拆分为 `ShellWorkspaceTabs.vue`、`ShellSidebarNav.vue`、`ShellBreadcrumb.vue`，导航分区条、左侧菜单和面包屑各自独立承载；旧 `src/config/sectionHomes.ts` 仅保留对 `src/utils/sectionWorkspaces.ts` 的兼容导出，避免再次出现双份导航配置
- **账号域继续下沉**：2026-03-20 起账号中心、实名认证、登录方式、修改密码四个抽屉已独立为 `ShellAccountDrawers.vue`，账号摘要/密码修改流程下沉到 `useShellAccountCenter.ts`，壳层视口与折叠状态下沉到 `useShellViewport.ts`，`AppShell` 仅保留导航编排与头部交互主线
- **头部交互继续下沉**：2026-03-20 起消息通知、帮助中心、命令面板、最近使用与快捷键监听已独立到 `useShellHeaderInteractions.ts`，`AppShell` 不再直接维护这组头部交互状态，继续朝“壳层只负责编排”的方向收口
- **导航装配继续下沉**：2026-03-20 起 `useShellNavigation.ts` 已统一承接游客态分组、动态菜单分组、静态兜底分组、当前激活分组、分组概览落点与一级分组切换逻辑；后续壳层新增导航规则时，优先扩展该 composable，不再回到 `AppShell` 内直接拼装分组
- **路由副作用继续下沉**：2026-03-20 起 `useShellRouteChangeEffects.ts` 已统一承接路由切换后的移动端菜单收起、头部浮层重置与账号抽屉关闭逻辑；后续壳层若再新增路由切换副作用，优先落到该 composable，避免 `AppShell` 回到“自己 watch 路由 + 自己做收口”的旧模式
- **壳层编排最终收口**：2026-03-20 起 `useShellOrchestrator.ts` 已统一装配 `useShellViewport`、`useShellAccountCenter`、`useShellNavigation`、`useShellHeaderInteractions`、`useShellRouteChangeEffects`；`AppShell` 只消费单一 orchestrator contract，后续壳层治理默认不再直接在主文件拼接多组 composable
- **壳层共享类型同步收口**：2026-03-20 起 `src/types/shell.ts` 已统一承接壳层组件 props、命令面板/通知面板数据结构与各 shell composable 返回 contract；后续壳层治理默认先改共享类型，再改实现，不再让组件层反向 import composable 类型
- **壳层组件级回归补齐**：2026-03-20 起已新增 `ShellWorkspaceTabs.test.ts`、`ShellSidebarNav.test.ts`、`ShellCommandPalette.test.ts`，分别覆盖一级工作台分区条、左侧二级菜单和全局命令面板的组件行为；壳层回归不再只停留在 composable 层
- **通知可读性**：消息通知入口增加未读数徽标，打开通知面板后自动标记为已读
- **移动端优化**：新增 `<=640px` 头部压缩模式，头像区仅保留图形位，降低窄屏拥挤
- **回归保障**：新增 `AppHeaderTools.test.ts` 与 `HeaderPopoverPanel.test.ts`，覆盖事件触发与面板点击跳转
- **壳层导航回归保障**：已新增 `useShellNavigation.test.ts`，覆盖游客态、动态菜单态、静态兜底态与分组落点跳转，防止一级/二级导航重构后再次出现“菜单可见但落点错误”一类回归
- **壳层路由副作用回归保障**：已新增 `useShellRouteChangeEffects.test.ts`，覆盖移动端与桌面态下的路由切换收口行为，防止后续壳层调整后再次出现抽屉残留、浮层未关闭或移动端菜单不收起的问题
- **壳层编排回归保障**：已新增 `useShellOrchestrator.test.ts`，覆盖壳层编排对头部交互参数与路由副作用参数的接线契约，防止继续拆分 `AppShell` 时出现“编排层少传/错传依赖”的回退
- **图表初始化优化**：`ReportAnalysisView` 图表改为视口内延迟初始化（IntersectionObserver），避免页面初始阶段一次性创建全部 ECharts 实例
- **构建告警治理**：`vite.config.ts` 中 ECharts 分包调整为 `vendor-echarts-core + vendor-zrender`，已消除大块与 empty chunk 告警
- **快捷入口**：已移除 `费用 / 备案 / 企业 / 支持` 四个头部入口，减少全局壳层噪音
- **首页状态条**：首页工具条已移除业务分组描述、接入环境与登录状态标签，仅保留核心操作入口，减少首屏噪音
- **头部工具轻量化**：2026-03-17 起顶部搜索框收口为短占位、圆角轻搜索框；消息通知与帮助中心收口为短标签入口，账号区默认仅展示头像与账号名，角色等信息下钻到悬浮面板
- **账号操作收口**：2026-03-17 起页面头部移除 `接入设置` 与显式 `退出登录` 按钮，统一改由右上角头像悬浮菜单承载账号中心、实名认证说明、登录方式、修改密码、退出登录
- **导航 schema 收口**：2026-03-20 起 `AppShell` fallback 导航、分组首页配置、角色默认落点、路由 `title/description` 已统一收口到 `src/utils/sectionWorkspaces.ts`，不再允许壳层、分组首页和路由表各自维护第二份菜单语义
- **命令面板替代搜索框**：2026-03-20 起顶部搜索升级为 `Ctrl/Cmd + K` 全局命令面板，按当前账号可见菜单和角色关注路径返回结果；头部保留轻量触发器，不再维持大输入框常驻占位
- **旧标签栏机制下线**：2026-03-20 起已移除 `trackTab` 路由策略、最近访问标签栏、`TabsView` 与 tabs store 遗留逻辑；功能页导航统一依赖面包屑、左侧菜单和命令面板
- **导航定位标准化**：2026-03-19 起左侧导航摘要继续收口为“当前工作台 + 二级菜单”极简结构，不再保留菜单数量、页面类型等解释型提示，避免导航本身抢占正文空间
- **一级顶栏标准化**：2026-03-18 起顶部导航重组为“品牌区 / 全局搜索 / 工具区 + 业务分区条”的双层结构，一级导航改为左对齐并统一激活态、悬浮提示与间距节奏，更贴近阿里云控制台的产品化布局
- **二级导航轻量化**：2026-03-17 起左侧二级导航正文仅保留菜单名称，说明改为悬浮提示，展开态短标识降为弱提示圆点，折叠态再保留缩写识别；激活态改为高亮底色 + 侧边指示线，减少左栏重复说明
- **页面标题区重构**：2026-03-19 起五个工作台下的功能页统一收口为仅保留阿里云式面包屑的极简页头；分组首页不再重复渲染壳层页头，功能页同步移除最近访问标签栏、主标题、同组横向导航及“返回分组概览 / 当前分区 / 页面类型 / 可见菜单”等低价值说明卡
- **卡片与按钮收口**：2026-03-17 起首页与分组首页统一收敛卡片圆角、阴影力度和入口按钮样式，减少“营销页式大圆角 / 重阴影”观感，进一步靠拢云控制台的克制风格
- **壳层防回退规则补充**：2026-03-20 起凡是修改 `ShellWorkspaceTabs`、`ShellSidebarNav`、`ShellCommandPalette`、`useShellNavigation`、`useShellRouteChangeEffects` 或 `useShellOrchestrator` 的 contract，都应同步更新对应组件/组合式单测；不再接受只改壳层实现、不补回归用例的收口方式
- **账号信息下钻**：头像菜单中的“实名认证”“登录方式”已补为独立右侧抽屉，账号中心同步展示账号类型、手机号、邮箱与实名状态
- **分组首页补齐**：已新增 `接入智维 / 风险运营 / 风险策略 / 平台治理 / 质量工场` 独立分组首页，一级导航点击后先进入概览页，再从概览页进入具体功能
- **分组首页精简**：2026-03-17 起分组首页进一步收口为“常用入口 / 最近使用 / 推荐操作 / 全部能力”四块，首屏不再重复展示路径提示与大段说明，更贴近云控制台的直觉浏览方式
- **查询容错**：告警运营台列表查询已增加 `status` 参数数值校验，前端仅在有效数字时透传，避免异常值触发后端参数绑定错误
- **ID 精度治理**：前端 API 层已引入 `IdType = string | number` 并统一替换 `id/*Id` 主键类型；`request.ts` 在 `JSON.parse` 前对 `id/*Id` 超大整数做字符串化兜底，后端 `Long` 主键也统一按字符串返回，避免 JS 精度丢失导致详情/关闭等操作错位
- **详情视图统一**：实时监测台 / GIS态势图、告警运营台、事件协同台、审计中心、异常观测台的“查看详情”入口已统一为右侧详情抽屉；抽屉采用共享 `StandardDetailDrawer` 外壳，统一页眉标签、分组信息卡、加载/空态/错误态表达，告别独立弹窗样式割裂
- **日志详情美化**：业务日志、系统日志与消息追踪详情进一步升级为“运行概览卡片 + 分组字段卡 + 深色报文块 + 结果提示卡”的统一视觉结构，长 TraceId、Topic、Payload 和异常信息在抽屉内可读性更强
- **业务详情美化**：告警运营台、事件协同台、风险监测详情同步升级为“概览卡片 + 业务分区 + 说明提示卡”结构，处置节点、责任人、风险对象与监测态势在抽屉内一屏可读
- **风险平台列表统一**：告警运营台、事件协同台、风险对象中心、阈值策略、联动编排、应急预案库、实时监测台、GIS态势图已统一为“KPI 概览卡 + 筛选卡 + 列表卡 / 资源卡 + 右侧详情抽屉”工作台结构；列表统一补齐操作栏、加载/空态表达，GIS 未定位对象改为资源卡呈现
- **业务工作台继续收口**：2026-03-18 起告警运营台、事件协同台、实时监测台、GIS态势图的提示条、列表操作条、空态卡与未定位资源卡统一收敛为浅色控制台式轻卡片风格，弱化厚边框与重阴影，提升列表区的一致性
- **详情抽屉继续收口**：2026-03-18 起 `StandardDetailDrawer` 进一步统一抽屉标题标签、概览卡、字段卡、提示卡与最近告警/事件卡片的圆角、阴影和状态胶囊样式，业务详情与日志详情保持同一套控制台视觉节奏
- **表单交互统一**：账号中心、角色权限、导航编排、组织架构、区域版图、数据字典、通知编排，以及风险对象中心、阈值策略、联动编排、应急预案库的新增/编辑入口已统一切换为右侧表单抽屉；风险对象中心绑定设备、事件协同台的派发/关闭、字典项管理及其新增/编辑也同步收口为抽屉；抽屉采用共享 `StandardFormDrawer` 外壳，统一页眉、副标题与底部动作区，保留原表单校验与提交逻辑
- **风险配置表单美化**：2026-03-18 起风险对象中心、阈值策略、联动编排、应急预案库的新增/编辑抽屉内部统一升级为“提示卡 + 分区卡 + 栅格字段”结构；风险对象中心绑定设备抽屉同步补齐只读信息、分区说明和轻卡片样式，录入层次与控制台工作台风格保持一致
- **工具交互统一**：自动化测试中心的“导入计划 / 新增自定义页面”以及页面盘点表格长文本列，连同全站复用的“导出列设置 / 模板命名 / 导入冲突处理 / 导入预览”，以及全局壳层的“账号中心 / 修改密码”也已统一切换到共享抽屉/文本列基座，避免工具型弹窗与业务抽屉风格割裂
- **KPI 卡统一**：自动化工场、运营分析中心等概况区开始收口到共享 `MetricCard`，减少各页面重复维护数值卡结构和局部样式
- **自动化工场指标卡继续收口**：`AutomationTestCenterView` 的“计划概况”和“页面盘点与脚手架生成”统计区都已切换到共享 `MetricCard`，不再保留页面私有 `metric-list / metric-item` 实现
- **自动化工场局部配色收口**：`AutomationTestCenterView` 的命令框、建议卡、场景卡、步骤卡、空态块与 Hero 标签芯片已改为复用 `--brand / --accent / --panel-border / --text-*` token，避免自动化工场继续保留页面私有浅蓝配色
- **自动化工场重复结构抽取**：`AutomationTestCenterView` 中“变量捕获”编辑块已提炼为 `src/components/AutomationCaptureEditor.vue`，用于统一标题、空态、输入行和新增/移除交互，降低场景编排页模板重复度
- **自动化工场步骤编排子组件化**：`AutomationTestCenterView` 中单步骤编辑表单已提炼为 `src/components/AutomationStepEditor.vue`，用于承接步骤头、定位配置、截图断言、接口触发、弹窗动作与变量捕获区，继续压缩页面顶层模板复杂度
- **自动化工场场景编排子组件化**：`AutomationTestCenterView` 中单场景编辑卡已提炼为 `src/components/AutomationScenarioEditor.vue`，统一承接场景基础信息、业务点梳理、首屏接口与步骤编排块，父页面仅保留编排状态与 shape 修正逻辑
- **自动化工场抽屉子组件化**：`AutomationTestCenterView` 中“导入计划”和“新增自定义页面”抽屉已提炼为 `src/components/AutomationPlanImportDrawer.vue`、`src/components/AutomationManualPageDrawer.vue`，父页面不再保留对应的输入缓存、关闭重置 watch 和页面级表单样式
- **自动化工场配置/盘点子组件化**：`AutomationTestCenterView` 中“执行配置”“测试建议”“页面盘点与脚手架生成”已提炼为 `src/components/AutomationExecutionConfigPanel.vue`、`src/components/AutomationSuggestionPanel.vue`、`src/components/AutomationPageDiscoveryPanel.vue`，父页面继续收口为状态编排层，盘点表格的选择桥接通过子组件暴露的选择方法完成
- **自动化工场状态编排 composable 化**：`AutomationTestCenterView` 的父级状态、盘点刷新/勾选、导入导出、场景编排与步骤 shape 修正逻辑已提炼为 `src/composables/useAutomationPlanBuilder.ts`，页面脚本只保留组件装配与返回值解构，降低后续继续拆分展示块时的耦合成本
- **按钮组统一**：产品工作台、数据校验台、链路验证中心、对象洞察台，以及设备资产中心的非冲突按钮区开始收口到共享 `StandardActionGroup`，减少 `button-row + 内联 style` 和页面私有 `action-row` 的重复实现
- **编辑器区块头统一**：自动化工场等复杂编排页的区块标题栏、场景卡头和步骤卡头开始收口到共享 `StandardInlineSectionHeader`，减少“标题 + 右侧轻操作”布局重复实现
- **摘要信息卡统一**：产品工作台、设备工作台、HTTP 上报工作台、文件调试页等查询结果/协议预演区域开始收口到共享 `StandardInfoGrid`，减少重复手写 `info-grid + info-chip` 结构，并同步修正共享摘要卡的窄屏响应式列数处理
- **流程导引统一**：HTTP 上报工作台、Future Lab 等步骤提示/路线桥接区域开始收口到共享 `StandardFlowRail`，避免继续在页面中复制 `flow-rail` 结构和索引样式
- **迁移收尾治理**：共享组件替换后，同步删除页面内失效的结构 class 与 scoped CSS，避免出现“模板切到标准件、局部样式仍维持旧实现”的分叉；`ProductWorkbenchView` 已完成首个 `info-chip` 清理样例
- **页面局部配色收口**：链路验证中心、对象洞察台、设备资产中心的局部卡片与状态横幅已由写死蓝色渐变切回 `--brand / --accent / --panel-border` token 体系，避免工作台继续各自维护一套蓝色主题
- **设备资产中心标准化**：`/devices` 已从单设备建档工作台重构为“Hero KPI + 筛选卡 + 列表卡 + 详情抽屉 + 表单抽屉 + 批量导入抽屉 + 设备更换抽屉”的标准列表页，统一接入 `StandardTableToolbar`、`StandardPagination`、`StandardDetailDrawer`、`StandardFormDrawer`、`StandardDrawerFooter` 与 CSV 导出列设置
- **拓扑入口成对治理**：凡是涉及父设备/子设备、网关/子节点等拓扑关系的资产页，必须同时补齐“新增/编辑/替换/导入入口”和“列表/详情展示入口”；不允许只在后端落库或只在详情展示其中一端。
- **产品定义中心标准化**：`/products` 已从联调型产品工作台重构为“单卡产品定义页 + 查询行动作区主按钮 + 卡内嵌筛选条 + 统计栏 + 列表区 + 详情抽屉 + 表单抽屉”的标准产品台账页，统一接入 `StandardTableToolbar`、`StandardPagination`、`StandardDetailDrawer`、`StandardFormDrawer`、`StandardDrawerFooter` 与 CSV 导出列设置；当前已移除 Hero 重复指标、独立筛选卡和台账次级标题，并把说明区与表格区合并到同一张主卡片中，后续新增产品物模型、批量导入等能力时不得回退到页面私有表单和请求面板
- **系统治理分页工程化**：组织、用户、角色、区域、字典、通知渠道、菜单、业务日志、系统日志已统一切换到后端 `/page` 真分页；树表页改为“根节点分页 + 子节点懒加载”，前端分页状态收敛到共享 `useServerPagination`，避免多页重复维护分页状态与翻页事件
- **分页组件全量统一**：2026-03-18 起，告警运营台、事件协同台、风险对象中心、阈值策略、联动编排、应急预案库，以及组织架构、账号中心、角色权限、导航编排、区域版图、数据字典、通知编排、审计中心、异常观测台、链路追踪台、实时监测台统一切换为共享 `StandardPagination`；`useServerPagination` 新增 `setTotal` 与 `applyLocalRecords`，用于在“后端真分页”和“本地分页切片”两类列表中复用同一分页契约
- **主色与导航高亮统一**：2026-03-18 起，`tokens.css` 与 `element-overrides.css` 主色映射统一到 `--brand`；`AppShell` 与 `AppHeaderTools` 的一级导航、二级导航高亮、头部工具态与输入焦点样式改为 token 变量驱动，减少页面间主色漂移
- **公共壳层视觉收口**：2026-03-18 起公共壳层进一步向阿里云式浅色控制台靠拢，统一背景层次、卡片阴影、侧栏选中态和页头信息密度，解决导航栏、左侧菜单与内容顶层各自为政的问题
- **共享壳层滚动规则统一**：2026-03-19 起 `AppShell` 统一切换为“顶部一级导航固定、左侧二级菜单固定、右侧内容区独立滚动”的视口布局；`接入智维 / 风险运营 / 风险策略 / 平台治理 / 质量工场` 下的功能页不再各自处理整页滚动，正文超出一屏时仅滚动内容区，左侧菜单保持固定
- **当前边界**：实名认证、登录方式当前采用账号抽屉形态承接，不单独新增路由；如后续纳入交付，再补齐个人资料、账号绑定与实名提交流程

#### 存在问题
1. **代码组织**：API调用分散在各组件中，缺乏统一的请求封装
2. **错误处理**：缺少统一的错误处理和提示机制
3. **性能优化**：通用懒加载与全站统一请求缓存仍未落地；当前仅 `/products` 已试点分页结果缓存、静默校验与下一页预取
4. **类型安全**：部分API返回类型不够明确
5. **用户体验**：缺少加载状态、空状态、错误状态的完整设计
6. **可维护性**：组件间通信依赖props，缺少统一的数据流管理
7. **防回退治理**：设备资产中心已完成标准列表页和第二轮抽屉能力重构；后续新增远程维护、维修工单联动等能力时，必须继续复用现有列表/抽屉/分页/导出基座，不得回退为页面私有工具条、弹窗和分页实现

### 1.3 技术栈评估

| 技术 | 版本 | 评估 |
|------|------|------|
| Vue | 3.5.30 | ✅ 现代化，Composition API |
| TypeScript | 5.8.3 | ✅ 类型安全 |
| Element Plus | 2.11.1 | ✅ 功能完整 |
| ECharts | 5.6.0 | ✅ 图表强大 |
| Vite | 7.0.0 | ✅ 构建快速 |
| Node | 24+ | ✅ 新版本支持 |

## 二、优化目标

### 2.1 核心目标
1. **提升代码质量**：遵循最佳实践，提高可维护性
2. **改善用户体验**：流畅的交互、清晰的状态反馈
3. **增强性能**：快速加载、智能缓存
4. **完善类型**：强类型约束、减少运行时错误

### 2.2 优化原则
- **渐进式优化**：分阶段实施，不影响现有功能
- **向后兼容**：不破坏现有API和页面
- **文档驱动**：每个优化都有对应的文档说明
- **测试覆盖**：关键功能有单元测试

## 三、优化方案

### 3.1 代码组织优化

#### 3.1.1 API层重构

**问题**：API调用分散在各组件中，缺少统一管理

**方案**：
```
src/
├── api/
│   ├── index.ts              # API统一出口
│   ├── request.ts            # 统一请求封装
│   ├── interceptors.ts       # 请求/响应拦截器
│   ├── product.ts            # 产品相关API
│   ├── device.ts             # 设备相关API
│   ├── message.ts            # 消息相关API
│   └── types.ts              # API类型定义
```

**实现要点**：
- 统一使用 axios 或 fetch 进行请求
- 实现请求/响应拦截器
- 统一错误处理
- 自动添加认证头
- 请求重试机制
- 历史兼容：`src/api/http.ts` 作为兼容入口时，会复用统一 `request.ts` 拦截器，并将旧路径（如 `/device/**`）自动归一为 `/api/device/**`，避免遗漏鉴权链路

```typescript
// src/api/request.ts
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';

const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    // 添加认证token
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse<ApiEnvelope<any>>) => {
    if (response.data.code !== 200) {
      // 统一错误处理
      ElMessage.error(response.data.msg || '请求失败');
      return Promise.reject(new Error(response.data.msg));
    }
    return response.data;
  },
  (error) => {
    // 网络错误处理
    ElMessage.error(error.message || '网络错误');
    return Promise.reject(error);
  }
);

export default api;
```

#### 3.1.2 组件库优化

**问题**：组件间通信依赖props，缺少统一的数据流管理

**方案**：
- 引入 Pinia 状态管理
- 实现组件通信的统一模式
- 建立组件库规范

```typescript
// src/stores/device.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { getDeviceByCode, getDeviceById } from '@/api/device';

export const useDeviceStore = defineStore('device', () => {
  const currentDevice = ref<Device | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const properties = computed(() => {
    if (!currentDevice.value) return [];
    // 计算属性逻辑
  });

  async function fetchDeviceByCode(deviceCode: string) {
    loading.value = true;
    error.value = null;
    try {
      const response = await getDeviceByCode(deviceCode);
      currentDevice.value = response.data;
      return response.data;
    } catch (err) {
      error.value = (err as Error).message;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  function clearDevice() {
    currentDevice.value = null;
    error.value = null;
  }

  return {
    currentDevice,
    loading,
    error,
    properties,
    fetchDeviceByCode,
    clearDevice
  };
});
```

### 3.2 性能优化

#### 3.2.1 路由懒加载

```typescript
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  {
    path: '/dashboard',
    component: () => import('@/views/DashboardView.vue')
  },
  {
    path: '/products',
    component: () => import('@/views/ProductView.vue')
  },
  {
    path: '/devices',
    component: () => import('@/views/DeviceView.vue')
  }
];

export const router = createRouter({
  history: createWebHistory(),
  routes
});
```

#### 3.2.2 图片和资源优化

- 使用 WebP 格式
- 实现图片懒加载
- SVG 图标使用

#### 3.2.3 请求缓存

```typescript
// src/utils/cache.ts
import { ref } from 'vue';

class CacheManager {
  private cache = new Map<string, { data: unknown; timestamp: number }>();
  private defaultTTL = 30000; // 30秒

  get<T>(key: string, ttl?: number): T | null {
    const item = this.cache.get(key);
    if (!item) return null;
    
    const effectiveTTL = ttl ?? this.defaultTTL;
    if (Date.now() - item.timestamp > effectiveTTL) {
      this.cache.delete(key);
      return null;
    }
    
    return item.data as T;
  }

  set<T>(key: string, data: T, ttl?: number): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    });
  }

  clear(key?: string): void {
    if (key) {
      this.cache.delete(key);
    } else {
      this.cache.clear();
    }
  }
}

export const cacheManager = new CacheManager();
```

### 3.3 类型安全增强

#### 3.3.1 API类型定义

```typescript
// src/api/types.ts
export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
}

export interface ProductListResponse extends ApiResponse<{
  records: Product[];
  total: number;
  size: number;
  current: number;
}> {}

export interface DeviceDetailResponse extends ApiResponse<Device> {}

export interface PropertyListResponse extends ApiResponse<DeviceProperty[]> {}

export interface MessageLogResponse extends ApiResponse<{
  records: DeviceMessageLog[];
  total: number;
  size: number;
  current: number;
}> {}
```

#### 3.3.2 组件Props类型

```typescript
// src/components/PropertyTrendPanel.vue
export interface PropertyTrendPanelProps {
  logs: DeviceMessageLog[];
  title?: string;
  showSummary?: boolean;
  maxSeries?: number;
}

export const props = defineProps<PropertyTrendPanelProps>({
  logs: {
    type: Array as PropType<DeviceMessageLog[]>,
    required: true
  },
  title: {
    type: String,
    default: '属性趋势预览'
  },
  showSummary: {
    type: Boolean,
    default: true
  },
  maxSeries: {
    type: Number,
    default: 4
  }
});
```

### 3.4 用户体验优化

#### 3.4.1 加载状态

```vue
<template>
  <div class="loading-container">
    <div v-if="loading" class="loading-spinner">
      <el-skeleton :rows="5" animated />
    </div>
    <div v-else-if="error" class="error-state">
      <el-alert :title="error" type="error" />
    </div>
    <div v-else-if="!data.length" class="empty-state">
      <el-empty description="暂无数据" />
    </div>
    <div v-else class="content">
      <slot :data="data" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  loading: boolean;
  error?: string | null;
  data: unknown[];
}>();
</script>
```

#### 3.4.2 空状态设计

```vue
<template>
  <div class="empty-state">
    <div class="empty-state__icon">
      <el-icon :size="64" color="#7284a5">
        <DataAnalysis />
      </el-icon>
    </div>
    <p class="empty-state__title">{{ title }}</p>
    <p class="empty-state__description">{{ description }}</p>
    <div v-if="action" class="empty-state__action">
      <el-button @click="action.callback">
        {{ action.label }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
interface EmptyStateProps {
  title?: string;
  description?: string;
  action?: {
    label: string;
    callback: () => void;
  };
}

defineProps<EmptyStateProps>();
</script>
```

#### 3.4.3 错误状态

```vue
<template>
  <div class="error-boundary">
    <div v-if="hasError" class="error-content">
      <el-alert
        :title="errorInfo.message"
        type="error"
        show-icon
      >
        <template #description>
          <pre class="error-stack">{{ errorInfo.stack }}</pre>
        </template>
      </el-alert>
      <el-button @click="resetError">
        重试
      </el-button>
    </div>
    <slot v-else />
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue';

interface ErrorInfo {
  message: string;
  stack: string;
}

const hasError = ref(false);
const errorInfo = ref<ErrorInfo | null>(null);

const emit = defineEmits<{
  (e: 'error', error: Error): void;
}>();

onErrorCaptured((error) => {
  hasError.value = true;
  errorInfo.value = {
    message: error.message,
    stack: error.stack || ''
  };
  emit('error', error);
  return false;
});

function resetError() {
  hasError.value = false;
  errorInfo.value = null;
}
</script>
```

### 3.5 测试覆盖

#### 3.5.1 单元测试

```typescript
// src/__tests__/api/device.test.ts
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { useDeviceStore } from '@/stores/device';

describe('useDeviceStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('should fetch device by code', async () => {
    const store = useDeviceStore();
    
    // Mock API
    vi.spyOn(api, 'get').mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 1,
        deviceCode: 'test-device',
        deviceName: 'Test Device'
      }
    });

    await store.fetchDeviceByCode('test-device');

    expect(store.currentDevice).toBeDefined();
    expect(store.currentDevice?.deviceCode).toBe('test-device');
  });

  it('should handle error when device not found', async () => {
    const store = useDeviceStore();
    
    vi.spyOn(api, 'get').mockRejectedValue(new Error('Device not found'));

    await expect(store.fetchDeviceByCode('not-exist')).rejects.toThrow();

    expect(store.error).toBe('Device not found');
    expect(store.currentDevice).toBeNull();
  });
});
```

#### 3.5.2 组件测试

```typescript
// src/__tests__/components/PropertyTrendPanel.test.ts
import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import PropertyTrendPanel from '@/components/PropertyTrendPanel.vue';

describe('PropertyTrendPanel', () => {
  it('renders correctly with logs', () => {
    const logs = [
      {
        id: 1,
        reportTime: '2024-01-01T00:00:00Z',
        payload: JSON.stringify({
          properties: { temperature: 25.5, humidity: 60 }
        })
      }
    ];

    const wrapper = mount(PropertyTrendPanel, {
      props: { logs }
    });

    expect(wrapper.text()).toContain('属性趋势预览');
  });

  it('shows empty state when no logs', () => {
    const wrapper = mount(PropertyTrendPanel, {
      props: { logs: [] }
    });

    expect(wrapper.text()).toContain('还没有足够的数值属性样本');
  });
});
```

### 3.6 开发体验优化

#### 3.6.1 ESLint + Prettier 配置

```json
// .eslintrc.json
{
  "extends": [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:vue/vue3-recommended",
    "prettier"
  ],
  "parser": "vue-eslint-parser",
  "parserOptions": {
    "parser": "@typescript-eslint/parser"
  },
  "plugins": [
    "@typescript-eslint",
    "vue"
  ],
  "rules": {
    "vue/multi-word-component-names": "off"
  }
}
```

```json
// .prettierrc
{
  "semi": false,
  "singleQuote": true,
  "tabWidth": 2,
  "useTabs": false,
  "arrowParens": "avoid",
  "trailingComma": "es5",
  "printWidth": 100
}
```

#### 3.6.2 Git Hooks

```json
// package.json
{
  "scripts": {
    "lint": "eslint src --ext .vue,.js,.ts,.jsx,.tsx",
    "lint:fix": "eslint src --ext .vue,.js,.ts,.jsx,.tsx --fix",
    "format": "prettier --write src"
  },
  "lint-staged": {
    "*.{vue,js,ts,jsx,tsx}": [
      "eslint --fix",
      "prettier --write"
    ]
  }
}
```

### 3.7 文档完善

#### 3.7.1 组件文档

```markdown
# PropertyTrendPanel

属性趋势预览组件，用于展示设备最近消息日志中的数值属性趋势。

## Props

| 参数 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| logs | 消息日志数组 | DeviceMessageLog[] | - |
| title | 标题 | string | '属性趋势预览' |
| showSummary | 是否显示摘要 | boolean | true |
| maxSeries | 最大显示序列数 | number | 4 |

## Events

| 事件名 | 说明 | 回调参数 |
|--------|------|----------|
| error | 发生错误时触发 | error: Error |

## 示例

```vue
<template>
  <PropertyTrendPanel :logs="logs" title="温度趋势" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import PropertyTrendPanel from '@/components/PropertyTrendPanel.vue';

const logs = ref<DeviceMessageLog[]>([]);
</script>
```
```

#### 3.7.2 API文档

```markdown
# API 文档

## 产品相关

### 获取产品列表

```typescript
import { getProductList } from '@/api/product';

const response = await getProductList({
  page: 1,
  size: 10,
  productName: 'test'
});

// response.data: {
//   records: Product[];
//   total: number;
//   size: number;
//   current: number;
// }
```

### 新增产品

```typescript
import { addProduct } from '@/api/product';

const response = await addProduct({
  productKey: 'demo-product',
  productName: '演示产品',
  protocolCode: 'mqtt-json',
  nodeType: 1
});

// response.data: Product
```

## 设备相关

### 获取设备详情

```typescript
import { getDeviceByCode } from '@/api/device';

const response = await getDeviceByCode('demo-device-01');

// response.data: Device
```

### 获取设备属性

```typescript
import { getDeviceProperties } from '@/api/device';

const response = await getDeviceProperties('demo-device-01');

// response.data: DeviceProperty[]
```
```

## 四、实施计划

### 4.1 阶段一：基础优化（1-2周）

- [ ] API层重构
- [ ] 组件库优化
- [ ] 路由懒加载
- [ ] 类型安全增强

### 4.2 阶段二：体验优化（1-2周）

- [ ] 加载状态优化
- [ ] 空状态设计
- [ ] 错误状态处理
- [ ] 性能优化

### 4.3 阶段三：测试与文档（1周）

- [ ] 单元测试
- [ ] 组件测试
- [ ] 文档完善
- [ ] 示例代码

### 4.4 阶段四：持续改进（长期）

- [ ] 性能监控
- [ ] 用户行为分析
- [ ] A/B测试
- [ ] 国际化支持

## 五、风险评估

### 5.1 技术风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 依赖升级导致兼容性问题 | 高 | 中 | 保持依赖版本稳定，定期测试 |
| 类型定义不完整 | 中 | 低 | 逐步完善类型定义 |
| 性能优化引入新问题 | 中 | 低 | 充分测试，性能监控 |

### 5.2 业务风险

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 优化影响现有功能 | 高 | 低 | 渐进式优化，充分测试 |
| 文档更新不及时 | 中 | 中 | 建立文档更新机制 |

## 六、总结

本优化方案从代码组织、性能、类型安全、用户体验、测试、开发体验六个维度提出了具体的优化措施。通过渐进式实施，可以在不影响现有功能的前提下，逐步提升代码质量和用户体验。

建议优先实施阶段一的基础优化，为后续工作打下坚实基础。
## 2026-03-18 Incremental Progress

- Pagination contract unification is completed in view layer: list pages now reuse `StandardPagination` and `useServerPagination`.
- Theme token unification is extended: hardcoded brand/accent colors in shared UI and high-frequency pages have been replaced by `var(--brand|--accent)` and `color-mix(...)`.
- Updated files in this batch include:
  - `src/styles/global.css`
  - `src/components/PanelCard.vue`
  - `src/components/TabsView.vue`
  - `src/views/SectionLandingView.vue`
  - `src/views/CockpitView.vue`
  - `src/views/ProductWorkbenchView.vue`
  - `src/views/DeviceWorkbenchView.vue`
  - `src/views/DeviceInsightView.vue`
  - `src/views/EventDisposalView.vue`
- Hardcoded color scan baseline (brand/accent literals) now remains only in `src/styles/tokens.css` as source token definitions.

## 2026-03-18 Stage 1 + Stage 2

- Stage 1 (theme baseline) completed:
  - `src/stores/theme.ts` is now token-driven.
  - Default theme colors are aligned with brand tokens (`#ff6a00` / `#ff8833`) instead of legacy blue defaults.
  - `applyTheme` now updates `--primary/--brand` token variables first, then keeps Element Plus variables aligned to the same token source.
  - `loadSystemPreference` keeps a stable light-mode acceptance baseline to avoid runtime color drift.
- Stage 2 (page skeleton unification) completed:
  - `PanelCard` now supports a `#header` slot so legacy page headers can be migrated without changing business structure.
  - Root list/workbench cards in system/governance/report pages have been migrated from direct `el-card` usage to `PanelCard`.
  - Current `views` status: `PanelCard` used by 27/28 pages; the only remaining `el-card` usage in `views` is KPI sub-cards inside `ReportAnalysisView` (non-root card fragments).
- Follow-up plan:
  - Stage 3 and Stage 4 are intentionally deferred for manual acceptance feedback before rollout.

## 2026-03-18 Stage 3 列表与分页统一

- 设备接入一级目录与系统治理一级目录的高频列表页已补齐统一列表容器 `standard-list-view`，列表外边距、筛选卡、表格区和分页区改为同一套全局规范。
- `MessageTraceView` 已切换为复用 `StandardPagination + useServerPagination`，与系统治理分页契约保持一致，不再单页维护独立分页状态。
- `MenuView` 已对齐系统治理列表模板：筛选区改用标准 `search-form`，补齐统一操作条和刷新入口，分页样式改用公共 `pagination` 区域。
- 已清理组织、用户、角色、区域、字典、通知渠道、系统日志、消息追踪等页面内重复的局部列表样式，避免同目录页面因局部 `scoped` 样式出现间距和观感漂移。
- 2026-03-18 起，组织、用户、角色、区域、字典、通知渠道、菜单及字典项子表统一启用表格单元格单行省略；超出列宽的内容默认悬停展示完整值，不再在列表中自动换行，交互与业务日志页保持一致。

## 2026-03-18 Frontend 修改问题沉淀

### 本次暴露的问题
- 在页面统一过程中，个别新增文案曾被错误编码写入 `MenuView.vue`，表现为按钮与统计文案出现 `褰撳墠缁撴灉`、`鍒锋柊鍒楄〃` 一类乱码。
- 列表页统一前，不同一级目录下同时存在全局样式、目录级样式和页面局部 `scoped` 样式三套口径，容易在新页面优化时继续复制出新的分页/列表差异。
- 构建校验阶段发现，页面优化任务之外的文件也可能阻塞前端整体构建，因此页面改造完成后不能只看局部页面效果，还要补做至少一次构建或类型校验。
- 产品定义中心继续暴露出另一类治理问题：普通功能页虽然已经切到标准列表骨架，但仍可能保留超长 Hero 说明、中英混排 eyebrow、全量展开筛选卡和重复 scoped 样式，导致首屏过高、信息密度失衡。

### 原因分析
- Windows 终端在非 UTF-8 口径下查看或复制中文内容时，容易把显示乱码误当成真实文本再次写回文件。
- 页面列表结构长期以“局部修补”为主，部分页面虽然已切换到 `PanelCard` 和 `StandardPagination`，但仍保留旧的局部样式覆盖，导致统一规范难以稳定收口。
- 缺少前端任务结束前的固定检查清单，导致编码、样式漂移、分页契约不一致等问题容易在新窗口重复出现。
- 早期工作台页以“解释页面作用”为主要目标，默认把说明文案、指标卡、筛选条件全部铺开；迁移到标准列表页后，如果不重新梳理首屏信息层级，就会把旧工作台式说明原样带进台账页。

### 后续防呆规则
- 前端文件编辑前后统一按 UTF-8 口径核对内容；在 Windows 终端中至少执行一次 UTF-8 方式查看文件，确认中文文案显示正常后再继续修改。
- 禁止把终端中显示异常的中文直接复制回源码；发现 `鍒�`、`褰�`、`璇�`、`鐢�`、`缁�` 一类字符串时，视为必须修复的问题，不能带着结束任务。
- 新的列表页或分页优化，必须优先复用 `PanelCard`、`StandardPagination`、`useServerPagination`、`standard-list-view` 与现有 design tokens；不再新增另一套页面私有列表骨架，除非文档中明确说明例外原因。
- 页面或样式改造完成后，至少补做一次 `git diff` 自检和一次前端构建/类型校验；若被其他现存文件阻塞，也要把阻塞文件和原因写入结果说明。
- 今后无论是否在新的对话窗口继续优化页面，只要涉及 `spring-boot-iot-ui` 的页面结构、列表样式、分页逻辑或中文文案，都必须先遵守本节规则，再开始编码。
- 普通台账页禁止继续保留大段 Hero 说明和中英混排 eyebrow；首屏只保留页面定位、结果量和关键操作，说明性文字应压缩到一句内或移入详情/文档。
- 当筛选项超过 3 个时，优先使用“常用条件 + 更多条件”的可展开结构；隐藏条件如果已命中，必须通过已启用筛选数、状态文案或标签显式提示，不能让高级条件静默生效。

## 2026-03-18 登录后菜单点击失效修复
- 问题现象：登录后可见菜单点击无效，页面会被路由守卫拦回首页或当前页。
- 根因：`AppShell` 在动态菜单缺失时会回退到静态导航分组，但 `permission` 路由权限校验仍仅依赖动态菜单 `allowedPaths`；同时历史菜单路径可能存在 `user`（无前导 `/`）等形态，导致渲染与鉴权路径不一致。
- 修复策略：新增统一路径归一化工具（补齐前导 `/`、移除尾部 `/`），用于菜单路径采集与导航激活判断；并在“已登录且有角色，但动态菜单路径为空”场景下，仅对标准分组中的静态页面启用受控兜底放行。
- 界面收口：`AppShell` 静态兜底菜单改为先按 `hasRoutePermission` 过滤后再渲染，避免出现“菜单可见但点击后被守卫回退”的假可用状态。
- 风险控制：无角色账号仍保持原有拦截行为；兜底范围限制在分组配置内页面，不放开未定义路径。
- 回归验证：新增 `routePath` 与 `permissionStoreRouteGuard` 单测，覆盖无前导斜杠菜单路径和空菜单树场景。
- 补充修复（同日）：一级导航分组按钮从“仅跨分组时跳转”改为“始终优先跳转该分组概览页”，例如在 `/products` 点击“设备接入”会进入 `/device-access`，避免体感“点击无响应”。

## 2026-03-19 页面组件治理基座补齐

- 新增共享组件：
  - `src/components/StandardTableToolbar.vue`：统一列表页工具条、统计信息和右侧操作入口。
  - `src/components/StandardTableTextColumn.vue`：统一表格长文本列的单行省略与悬浮完整展示。
  - `src/components/StandardDrawerFooter.vue`：统一抽屉底部确认/取消按钮布局与样式。
  - `src/components/StandardInfoGrid.vue`：统一工作台查询结果、协议预演与只读摘要卡的标签/值网格布局。
  - `src/components/StandardFlowRail.vue`：统一流程导引、发送后检查与路线桥接等步骤型内容的索引、标题和说明布局。
  - `src/components/StandardInlineSectionHeader.vue`：统一编辑器、编排页和复杂表单中的区块标题与右侧轻操作入口。
  - `src/components/StandardActionGroup.vue`：统一工作台、表单区和编排页按钮组的排列、换行与常用间距。
  - `src/utils/confirm.ts`：统一删除、确认、关闭、抑制等二次确认调用口径。
- 共享抽屉收口：
  - `StandardDetailDrawer`、`StandardFormDrawer` 已对齐全局 `brand/accent` token，不再保留页面外观之外的独立蓝色抽屉主题。
  - 统一修复了详情抽屉默认文案里的乱码风险，详情空态与加载态保持 UTF-8 可读。
- 页面迁移进展：
  - `MenuView` 已切换到 `StandardTableToolbar + StandardTableTextColumn + StandardDrawerFooter + confirmDelete`。
  - `AlarmCenterView`、`EventDisposalView` 已切换到 `StandardTableToolbar + StandardTableTextColumn + confirmAction`，处置类页面的二次确认文案与交互入口保持一致。
  - `UserView`、`RoleView`、`OrganizationView`、`RegionView`、`DictView`、`ChannelView` 已切换到 `StandardTableToolbar + StandardTableTextColumn + StandardDrawerFooter + confirmDelete`，系统治理高频页不再保留各自的列表工具条、文本溢出与抽屉底部按钮实现。
  - `RealTimeMonitoringView` 已切换到 `StandardTableToolbar + StandardTableTextColumn + useServerPagination`，监测列表与系统治理页复用同一分页契约和溢出提示策略。
  - `MessageTraceView`、`AuditLogView`、`RiskGisView` 已切换到 `StandardTableToolbar + StandardTableTextColumn`；其中日志删除确认统一切换到共享 `confirmAction`。
  - `RiskPointView`、`RuleDefinitionView`、`LinkageRuleView`、`EmergencyPlanView` 已切换到 `StandardTableToolbar + StandardTableTextColumn + StandardDrawerFooter + confirmDelete`，风险配置页的抽屉底部动作与删除交互口径保持一致。
  - `AutomationTestCenterView` 已把页面盘点表格中的长文本列切换到 `StandardTableTextColumn`，并把“导入计划 / 新增自定义页面”抽屉底部动作切换到 `StandardDrawerFooter`，避免继续保留原生 `show-overflow-tooltip` 与散装底部按钮实现。
  - `AutomationTestCenterView` 已把“计划概况”切换到共享 `MetricCard`，自动化编排页不再维护独立的概况数值卡模板。
- `AutomationTestCenterView` 已把场景编排编辑器中的“业务点梳理 / 首屏接口 / 步骤编排 / 场景卡头 / 步骤卡头 / 变量捕获”区块头切换到 `StandardInlineSectionHeader`，复杂编辑区不再重复手写相同的标题栏结构。
- `AutomationTestCenterView`、`ProductWorkbenchView`、`FilePayloadDebugView`、`ReportWorkbenchView`、`DeviceInsightView` 以及 `DeviceWorkbenchView` 的非冲突按钮区已切换到 `StandardActionGroup`，工作台和编排页不再继续复制 `button-row + style`、`action-row + 局部 class`。
  - `DeviceInsightView` 已把设备属性快照表格切换到 `StandardTableTextColumn`，风险对象洞察页的标识符、属性值和时间列统一采用共享溢出展示策略。
  - `ProductWorkbenchView`、`ReportWorkbenchView`、`DeviceWorkbenchView`、`FilePayloadDebugView` 已把查询结果摘要卡、协议预演摘要卡和校验概况卡切换到 `StandardInfoGrid`，工作台类页面不再重复手写相同的 `info-grid/info-chip` 模板。
  - `ReportAnalysisView` 已把页首 KPI 指标卡切换到共享 `MetricCard`，运营分析中心不再保留独立 `el-row + el-card` 数值卡实现。
  - `ReportWorkbenchView`、`FutureLabView` 已把“发送后建议检查”“与项目文档的衔接”等步骤导引区域切换到 `StandardFlowRail`，流程提示不再由页面局部 `flow-rail` 模板各自维护。
  - `SectionLandingView` 已移除局部蓝色主视觉，统一对齐全局 brand/accent token，避免“治理概览 / 处置概览”与整体方案配色割裂。
- 本轮新增防呆规则：
  - 新的列表页优化不得再直接手写一套 `table-action-bar + show-overflow-tooltip + ElMessageBox.confirm + 抽屉底部按钮` 组合。
  - 详情页、表单页、二次确认弹窗与概览页都必须优先走共享 token 和共享组件；如需例外，必须在本文件记录原因与范围。
  - 普通列表页如果需要首屏概况卡，优先复用共享 `MetricCard` 并根据页面密度切到紧凑规格，不再额外新建 `mini-metric-card`、`summary-chip-grid` 一类私有统计卡。
  - 普通列表页的筛选区默认先暴露常用条件；“更多条件”必须支持展开/收起，并在收起态保留当前筛选命中的可感知反馈。
- 后续未完成项：
- 自动化测试中心的场景/步骤编排编辑器仍保留少量页内局部按钮布局，后续如继续抽象需避免牺牲编排效率和现有交互密度。
- `DeviceWorkbenchView` 顶部 Hero 按钮区与现有命名改动落在同一片段，当前暂未并入 `StandardActionGroup`；后续若继续处理，应先再次做同片段冲突判断。
  - 后续新增页面和局部重构任务仍可能绕过共享基座直接手写工具栏、确认弹窗和抽屉底部按钮，需要继续用文档和代码评审防止回退。

## 2026-03-20 `/devices` 替换抽屉收口

- 本轮已补齐 `/devices` 替换抽屉的“摘要预填秒开 + 后台补全最新档案 + 脏表单保护 + 请求过期保护 + 顶部轻量状态提示”闭环；打开替换表单时不再等待完整详情和下拉选项全部返回。
- 替换抽屉现在会优先复用列表摘要和本地详情缓存秒开；后台补数取回后，若用户尚未开始填写表单则自动补齐最新源设备档案，若已开始输入则保留当前表单并给出轻量提示，避免异步覆盖。
- 页面布局结论已更新：`/devices` 已与 `/products` 对齐为单主卡紧凑结构，顶部筛选改为“默认 3 项 + 可展开更多条件”的同卡结构，并把新增/导入并入查询行。
- 下一步更高价值的优化建议转向“批量导入成功后的当前页本地回写 + 静默校验”，优先继续减少批量导入场景抽屉关闭后的列表跳闪。
