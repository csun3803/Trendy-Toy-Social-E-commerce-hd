"""
本地FAQ知识库
策略：常见问题（如"怎么退换货"）直接本地匹配返回，省Token且响应快
匹配算法：关键词精确匹配 + 字符相似度（Jaccard）混合打分
"""
from dataclasses import dataclass, field


@dataclass
class FaqItem:
    keywords: list[str]
    patterns: list[str] = field(default_factory=list)  # 典型问法
    answer: str = ""

    def score(self, message: str) -> float:
        """返回0~1的匹配置信度"""
        if not message:
            return 0.0
        msg = message.lower()
        # 关键词命中
        kw_hit = sum(1 for k in self.keywords if k.lower() in msg)
        kw_score = kw_hit / max(len(self.keywords), 1)

        # 典型问法相似度（Jaccard，分词按字符）
        pat_score = 0.0
        if self.patterns:
            for p in self.patterns:
                pat_score = max(pat_score, _jaccard(msg, p.lower()))

        # 综合分（关键词权重高）
        return max(kw_score * 0.7 + pat_score * 0.3, kw_score)


def _jaccard(a: str, b: str) -> float:
    """字符级Jaccard相似度，简单快速，无需分词库"""
    sa = set(a)
    sb = set(b)
    if not sa or not sb:
        return 0.0
    return len(sa & sb) / len(sa | sb)


# ============= FAQ知识库 =============
FAQ_KB: list[FaqItem] = [
    FaqItem(
        keywords=["退换货", "退货", "退款", "退钱", "换货", "怎么退"],
        patterns=["怎么退换货", "如何退款", "我要退货", "怎么退款", "怎么换货"],
        answer=(
            "关于退换货：\n"
            "1. 在「订单详情」中点击「申请售后」\n"
            "2. 选择退款/退货原因并提交\n"
            "3. 商家审核通过后，退款将在3-5个工作日内原路返回\n"
            "4. 退货商品需保持未拆封状态\n"
            "5. 盲盒拆封后不支持退换，请谨慎购买"
        ),
    ),
    FaqItem(
        keywords=["发货", "快递", "物流", "配送", "多久到", "什么时候到", "运费"],
        patterns=["什么时候发货", "多久到", "怎么查物流", "发货时间"],
        answer=(
            "关于发货/物流：\n"
            "1. 商家一般会在下单后48小时内发货\n"
            "2. 发货后可在「订单详情」中查看物流信息\n"
            "3. 一般3-7天可送达，偏远地区可能需要更长时间\n"
            "4. 如超时未发货，可联系商家或申请退款"
        ),
    ),
    FaqItem(
        keywords=["盲盒", "概率", "隐藏款", "抽盒", "保底", "整盒"],
        patterns=["盲盒概率", "隐藏款概率", "抽盒规则", "整盒多少钱"],
        answer=(
            "关于盲盒/抽盒：\n"
            "1. 盲盒为随机抽取，每个款式概率不同\n"
            "2. 常规款概率均等，隐藏款概率通常为1/144\n"
            "3. 整盒购买可集齐所有常规款（不含隐藏款）\n"
            "4. 拆封后不支持退换，请谨慎购买"
        ),
    ),
    FaqItem(
        keywords=["支付", "付款", "微信", "支付宝", "怎么付", "银行卡"],
        patterns=["怎么付款", "支持什么支付", "可以用支付宝吗"],
        answer=(
            "关于支付方式：\n"
            "目前支持微信支付、支付宝等主流支付方式，下单后选择您方便的支付方式即可完成付款。"
        ),
    ),
    FaqItem(
        keywords=["优惠券", "折扣", "满减", "活动", "红包", "优惠码"],
        patterns=["怎么用优惠券", "有什么活动", "满减规则"],
        answer=(
            "关于优惠活动：\n"
            "1. 优惠券可在「我的-优惠券」中查看\n"
            "2. 下单时选择可用优惠券即可抵扣\n"
            "3. 每单限用一张优惠券\n"
            "4. 关注店铺可获取专属优惠"
        ),
    ),
    FaqItem(
        keywords=["注册", "登录", "账号", "密码", "忘记密码", "改密码", "登录不了"],
        patterns=["忘记密码", "怎么注册", "登录不了"],
        answer=(
            "关于账号问题：\n"
            "1. 支持手机号注册登录\n"
            "2. 忘记密码可通过手机验证码重置\n"
            "3. 如遇登录问题，请联系客服"
        ),
    ),
    FaqItem(
        keywords=["账号注销", "注销账号", "注销", "删除账号", "取消账号", "怎么注销"],
        patterns=["怎么注销账号", "账号注销", "我要注销", "如何注销", "注销流程"],
        answer=(
            "关于账号注销：\n"
            "1. 在App「我的-设置-账号与安全」中找到「注销账号」入口\n"
            "2. 注销前请确保：无未完成订单、无待处理售后、账户余额为零\n"
            "3. 注销后账号数据将不可恢复，请谨慎操作\n"
            "4. 如无法自助注销，请联系人工客服协助处理"
        ),
    ),
    FaqItem(
        keywords=["收藏", "关注", "喜欢", "我的心愿单"],
        patterns=["怎么收藏", "收藏在哪看", "怎么关注"],
        answer=(
            "关于收藏功能：\n"
            "1. 在商品详情页点击心形图标即可收藏\n"
            "2. 收藏的商品可在「我的收藏」中查看\n"
            "3. 收藏的商品降价时会收到通知"
        ),
    ),
    FaqItem(
        keywords=["售后", "投诉", "质量问题", "损坏", "破损", "瑕疵", "少件"],
        patterns=["商品破损", "质量有问题", "怎么投诉"],
        answer=(
            "关于售后问题：\n"
            "1. 收到商品7天内可申请售后\n"
            "2. 质量问题请拍照留证后联系商家\n"
            "3. 运输损坏请在签收时拒收并联系客服\n"
            "4. 如商家不处理，可申请平台介入"
        ),
    ),
    FaqItem(
        keywords=["盒柜", "展示柜", "我的盒柜", "陈列"],
        patterns=["盒柜怎么用", "怎么展示"],
        answer=(
            "关于盒柜功能：\n"
            "1. 盒柜用于展示您收藏的潮玩\n"
            "2. 可在「我的-我的盒柜」中添加和管理\n"
            "3. 支持创建多个主题柜\n"
            "4. 可分享盒柜给其他玩家欣赏"
        ),
    ),
    FaqItem(
        keywords=["商家", "开店", "入驻", "卖东西", "上架"],
        patterns=["怎么开店", "如何入驻", "商家入驻流程"],
        answer=(
            "关于商家入驻：\n"
            "1. 点击「商家入驻」申请开店\n"
            "2. 需要提供营业执照等资质\n"
            "3. 平台审核通过后即可上架商品\n"
            "4. 平台收取一定比例的技术服务费"
        ),
    ),
    FaqItem(
        keywords=["推荐", "猜你喜欢", "智能推荐", "个性化"],
        patterns=["怎么推荐", "猜你喜欢怎么算"],
        answer=(
            "关于AI智能推荐：\n"
            "1. 系统会根据您的浏览、收藏和购买记录推荐商品\n"
            "2. 推荐越用越精准\n"
            "3. 您可以在首页「猜你喜欢」板块查看推荐\n"
            "4. 也可以在商品详情页查看相似推荐"
        ),
    ),
]


DEFAULT_REPLY = (
    "感谢您的咨询！我暂时无法精准理解您的问题，您可以尝试：\n"
    "1. 换个方式描述您的问题\n"
    "2. 选择以下常见问题：\n"
    "   - 退款/退货\n"
    "   - 发货/物流\n"
    "   - 盲盒/抽盒\n"
    "   - 支付方式\n"
    "   - 优惠券\n"
    "   - 售后问题\n"
    "3. 如需人工客服，请拨打客服电话"
)


def match_faq(message: str, threshold: float = 0.35) -> tuple[FaqItem | None, float]:
    """
    在FAQ知识库中匹配最高分项
    返回 (FaqItem | None, 置信度)
    """
    if not message or not message.strip():
        return None, 0.0
    best: FaqItem | None = None
    best_score = 0.0
    for item in FAQ_KB:
        s = item.score(message)
        if s > best_score:
            best_score = s
            best = item
    if best is None or best_score < threshold:
        return None, best_score
    return best, best_score
