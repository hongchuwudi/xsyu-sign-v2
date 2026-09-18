package com.hongchu.qqrobotsign.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hongchu.qqrobotsign.config.props.OssStorageProperties;
import com.hongchu.qqrobotsign.context.BaseContext;
import com.hongchu.qqrobotsign.exception.BusinessException;
import com.hongchu.qqrobotsign.mapper.ContentAssetRefMapper;
import com.hongchu.qqrobotsign.mapper.MediaAssetMapper;
import com.hongchu.qqrobotsign.pojo.VO.MediaAssetUploadVO;
import com.hongchu.qqrobotsign.pojo.entity.ContentAssetRef;
import com.hongchu.qqrobotsign.pojo.entity.MediaAsset;
import com.hongchu.qqrobotsign.service.MediaAssetService;
import com.hongchu.qqrobotsign.storage.ObjectStorageService;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAssetServiceImpl extends ServiceImpl<MediaAssetMapper, MediaAsset> implements MediaAssetService {
    private static final String ANNOUNCEMENT = "ANNOUNCEMENT";
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final MediaAssetMapper assetMapper;
    private final ContentAssetRefMapper refMapper;
    private final ObjectStorageService objectStorageService;
    private final OssStorageProperties properties;
    private final Parser markdownParser = Parser.builder().build();

    @Override
    public MediaAssetUploadVO upload(MultipartFile file, String draftToken) {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择图片文件");
        if (file.getSize() > properties.maxFileSizeBytes()) throw new BusinessException("图片超过上传大小限制");
        try {
            byte[] bytes = file.getBytes();
            String contentType = detectType(bytes);
            String extension = extension(contentType);
            String normalizedDraftToken = normalizeDraftToken(draftToken);
            String date = java.time.LocalDate.now().toString().replace('-', '/');
            String objectKey = trimSlash(properties.getObjectPrefix()) + "/" + date + "/" + UUID.randomUUID() + "." + extension;
            objectStorageService.put(objectKey, new ByteArrayInputStream(bytes), bytes.length, contentType);
            try {
                MediaAsset asset = new MediaAsset();
                asset.setObjectKey(objectKey);
                asset.setPublicUrl(trimSlash(properties.getPublicBaseUrl()) + "/" + objectKey);
                String originalName = file.getOriginalFilename() == null ? "image." + extension : file.getOriginalFilename();
                asset.setOriginalName(originalName.substring(0, Math.min(255, originalName.length())));
                asset.setContentType(contentType);
                asset.setFileSize((long) bytes.length);
                asset.setFileHash(sha256(bytes));
                asset.setStatus("TEMP");
                asset.setDraftToken(normalizedDraftToken);
                asset.setCreatedBy(BaseContext.getCurrentId());
                asset.setCreatedAt(LocalDateTime.now());
                asset.setUpdatedAt(LocalDateTime.now());
                assetMapper.insert(asset);
                return new MediaAssetUploadVO(asset.getId(), asset.getPublicUrl(), asset.getOriginalName(), contentType, asset.getFileSize());
            } catch (RuntimeException e) {
                try {
                    objectStorageService.delete(objectKey);
                } catch (RuntimeException cleanupError) {
                    log.warn("媒体元数据保存失败，回滚 OSS 对象失败: {}", objectKey, cleanupError);
                }
                throw e;
            }
        } catch (IOException e) {
            throw new BusinessException("读取图片失败");
        } catch (RuntimeException e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void syncAnnouncementReferences(Long announcementId, String markdown, String draftToken) {
        Set<String> urls = extractImageUrls(markdown);
        List<MediaAsset> assets = urls.isEmpty() ? List.of() : assetMapper.selectList(new LambdaQueryWrapper<MediaAsset>().in(MediaAsset::getPublicUrl, urls));
        Set<Long> desired = new HashSet<>();
        for (MediaAsset asset : assets) {
            if ("TEMP".equals(asset.getStatus())
                    && (!Objects.equals(asset.getDraftToken(), normalizeDraftToken(draftToken))
                    || !Objects.equals(asset.getCreatedBy(), BaseContext.getCurrentId()))) {
                throw new BusinessException("公告引用了不属于当前草稿的图片");
            }
            desired.add(asset.getId());
            asset.setStatus("ACTIVE");
            asset.setDraftToken(null);
            asset.setOrphanedAt(null);
            asset.setUpdatedAt(LocalDateTime.now());
            assetMapper.updateById(asset);
        }

        List<ContentAssetRef> existing = refMapper.selectList(new LambdaQueryWrapper<ContentAssetRef>()
                .eq(ContentAssetRef::getBizType, ANNOUNCEMENT).eq(ContentAssetRef::getBizId, announcementId));
        for (ContentAssetRef ref : existing) {
            if (!desired.contains(ref.getAssetId())) {
                refMapper.deleteById(ref.getId());
                markOrphanIfUnreferenced(ref.getAssetId());
            } else {
                desired.remove(ref.getAssetId());
            }
        }
        for (Long assetId : desired) {
            ContentAssetRef ref = new ContentAssetRef();
            ref.setBizType(ANNOUNCEMENT);
            ref.setBizId(announcementId);
            ref.setAssetId(assetId);
            ref.setCreatedAt(LocalDateTime.now());
            refMapper.insert(ref);
        }
    }

    @Override
    @Transactional
    public void markAnnouncementOrphaned(Long announcementId) {
        List<ContentAssetRef> refs = refMapper.selectList(new LambdaQueryWrapper<ContentAssetRef>()
                .eq(ContentAssetRef::getBizType, ANNOUNCEMENT).eq(ContentAssetRef::getBizId, announcementId));
        refs.forEach(ref -> refMapper.deleteById(ref.getId()));
        refs.forEach(ref -> markOrphanIfUnreferenced(ref.getAssetId()));
    }

    private void markOrphanIfUnreferenced(Long assetId) {
        long count = refMapper.selectCount(new LambdaQueryWrapper<ContentAssetRef>().eq(ContentAssetRef::getAssetId, assetId));
        if (count == 0) {
            MediaAsset asset = assetMapper.selectById(assetId);
            if (asset != null && !"ORPHAN".equals(asset.getStatus())) {
                asset.setStatus("ORPHAN");
                asset.setOrphanedAt(LocalDateTime.now());
                asset.setUpdatedAt(LocalDateTime.now());
                assetMapper.updateById(asset);
            }
        }
    }

    private Set<String> extractImageUrls(String markdown) {
        Set<String> urls = new HashSet<>();
        if (markdown == null || markdown.isBlank()) return urls;
        Node document = markdownParser.parse(markdown);
        NodeVisitor visitor = new NodeVisitor(new VisitHandler<>(Image.class, image -> urls.add(image.getUrl().toString())));
        visitor.visit(document);
        return urls;
    }

    private String detectType(byte[] bytes) {
        String type = null;
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) type = "image/jpeg";
        else if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) type = "image/png";
        else if (bytes.length >= 6 && ((bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a'))) type = "image/gif";
        else if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') type = "image/webp";
        if (!ALLOWED_TYPES.contains(type)) throw new BusinessException("仅支持 JPEG、PNG、GIF、WebP 图片，禁止 SVG");
        return type;
    }

    private String extension(String contentType) { return contentType.substring(contentType.indexOf('/') + 1).replace("jpeg", "jpg"); }
    private String trimSlash(String value) { return value == null ? "" : value.replaceAll("^/+|/+$", ""); }
    private String normalizeDraftToken(String value) {
        if (value == null || value.isBlank()) return null;
        String token = value.trim();
        if (token.length() > 64) throw new BusinessException("draftToken 长度不能超过 64 个字符");
        return token;
    }
    private String sha256(byte[] bytes) {
        try { byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes); StringBuilder sb = new StringBuilder(); for (byte b : digest) sb.append(String.format("%02x", b)); return sb.toString(); }
        catch (Exception e) { throw new IllegalStateException("无法计算图片摘要", e); }
    }

    @Scheduled(cron = "${xsyu.storage.cleanup-cron:0 30 3 * * *}")
    public void cleanupExpiredAssets() {
        LocalDateTime now = LocalDateTime.now();
        List<MediaAsset> assets = assetMapper.selectList(new LambdaQueryWrapper<MediaAsset>()
                .and(w -> w.eq(MediaAsset::getStatus, "TEMP").lt(MediaAsset::getCreatedAt, now.minusHours(24)))
                .or(w -> w.eq(MediaAsset::getStatus, "ORPHAN").lt(MediaAsset::getOrphanedAt, now.minusDays(7))));
        for (MediaAsset asset : assets) {
            if (refMapper.selectCount(new LambdaQueryWrapper<ContentAssetRef>().eq(ContentAssetRef::getAssetId, asset.getId())) > 0) continue;
            try { objectStorageService.delete(asset.getObjectKey()); assetMapper.deleteById(asset.getId()); }
            catch (RuntimeException e) { log.warn("清理 OSS 图片失败: {}", asset.getObjectKey(), e); }
        }
    }
}
