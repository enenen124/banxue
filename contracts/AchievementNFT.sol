// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title 伴学成就NFT
 * @notice 5种学习成就，每种每个用户最多铸造1次
 *
 * 成就ID对应关系：
 *   0 = 初来乍到（完成第1次专注）
 *   1 = 学习达人（累计学习10小时）
 *   2 = 百炼成钢（累计获得100积分）
 *   3 = 笔耕不辍（发布第1篇帖子）
 *   4 = 人见人爱（获得10个赞）
 */
contract AchievementNFT is ERC721, Ownable {

    // ---------- 状态变量 ----------

    uint256 private _tokenIdCounter; // 自增token编号

    // 某用户是否已铸造某成就
    mapping(address => mapping(uint8 => bool)) public hasMinted;

    // 每个token对应哪个成就ID
    mapping(uint256 => uint8) public tokenAchievement;

    // ---------- 事件 ----------

    // 铸造成功时触发，前端监听用
    event AchievementMinted(
        address indexed user,
        uint8 indexed achievementId,
        uint256 indexed tokenId
    );

    // ---------- 构造函数 ----------

    constructor() ERC721("Banxue Achievement", "BAXUE") Ownable(msg.sender) {}

    /**
     * @notice 铸造一个成就NFT
     * @param achievementId 成就ID（0-4）
     *
     * 调用前确保：
     *  1. 用户已连接 MetaMask（Sepolia 网络）
     *  2. 钱包有足够的 Sepolia ETH 支付 gas
     *  3. 该成就尚未铸造过
     */
    function mint(uint8 achievementId) external {
        // 校验1：成就ID必须在 0~4 范围内
        require(achievementId < 5, "Invalid achievement ID");

        // 校验2：该用户不能重复铸造同一成就
        require(!hasMinted[msg.sender][achievementId], "Already minted");

        // 铸造
        _tokenIdCounter++;
        uint256 newTokenId = _tokenIdCounter;

        _safeMint(msg.sender, newTokenId);
        tokenAchievement[newTokenId] = achievementId;
        hasMinted[msg.sender][achievementId] = true;

        emit AchievementMinted(msg.sender, achievementId, newTokenId);
    }

    /**
     * @notice 返回该token的元数据URL（OpenSea等平台用）
     * @dev 由后端动态生成JSON，基础URL部署时设置
     */
    function tokenURI(uint256 tokenId)
        public
        view
        override
        returns (string memory)
    {
        _requireOwned(tokenId); // ERC721自带：检查token是否存在
        uint8 achId = tokenAchievement[tokenId];

        // 拼接元数据URL，如 /api/achievements/metadata/0
        return
            string(
                abi.encodePacked(
                    "https://your-backend-url/api/achievements/metadata/",
                    _toString(achId),
                    "/",
                    _toString(tokenId)
                )
            );
    }

    /**
     * @notice 查询某用户已铸造的所有成就ID列表
     * @dev 前端调用来决定哪些成就显示"已上链"
     */
    function getMintedAchievements(address user)
        external
        view
        returns (uint8[] memory)
    {
        // 最多5个，先建临时数组
        uint8[] memory temp = new uint8[](5);
        uint256 count = 0;
        for (uint8 i = 0; i < 5; i++) {
            if (hasMinted[user][i]) {
                temp[count] = i;
                count++;
            }
        }
        // 裁剪到实际长度
        uint8[] memory result = new uint8[](count);
        for (uint256 i = 0; i < count; i++) {
            result[i] = temp[i];
        }
        return result;
    }

    // ---------- 工具函数 ----------

    // 把 uint256 转成字符串（Solidity 没有内置 toString）
    function _toString(uint256 value) internal pure returns (string memory) {
        if (value == 0) return "0";
        uint256 temp = value;
        uint256 digits;
        while (temp != 0) {
            digits++;
            temp /= 10;
        }
        bytes memory buffer = new bytes(digits);
        while (value != 0) {
            digits--;
            buffer[digits] = bytes1(uint8(48 + (value % 10)));
            value /= 10;
        }
        return string(buffer);
    }
}

// 0xA928CD1fd83Bc8b422c3fca8027caF0496db0E33
