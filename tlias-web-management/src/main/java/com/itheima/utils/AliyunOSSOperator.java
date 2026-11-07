package com.itheima.utils;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.EnvironmentVariableCredentialsProvider;
import com.aliyun.sdk.service.oss2.models.PutObjectRequest;
import com.aliyun.sdk.service.oss2.transport.BinaryData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 阿里云 OSS 操作工具类（基于 OSS Java SDK V2）
 * 访问凭证从环境变量 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET 读取，不写在配置文件里
 */
@Component
public class AliyunOSSOperator {

    /**
     * 对象名的年月目录格式，如 2026/09/
     */
    private static final DateTimeFormatter DATE_FOLDER = DateTimeFormatter.ofPattern("yyyy/MM/");

    /**
     * 重名时最多尝试的序号，兜底防止死循环
     */
    private static final int MAX_RENAME_ATTEMPTS = 100;

    /**
     * 常见扩展名与 Content-Type 的对应关系，未命中时按二进制流处理
     */
    private static final Map<String, String> CONTENT_TYPE_MAP = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("json", "application/json"),
            Map.entry("xml", "application/xml"),
            Map.entry("html", "text/html"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("zip", "application/zip")
    );

//    @Value("${aliyun.oss.region}")
//    private String region;
//
//    @Value("${aliyun.oss.endpoint}")
//    private String endpoint;
//
//    @Value("${aliyun.oss.bucketName}")
//    private String bucketName;

    @Autowired
    private AliyunOssProperties aliyunOssProperties;

    private OSSClient ossClient;

    @PostConstruct
    public void init() {
        //OSSClient 创建开销较大，初始化一次后全局复用
        //region 不能省：SDK 默认用 V4 签名，签名串里必须带上地域，否则服务端返回 Invalid signing region
        //endpoint 决定请求发往哪个地址，region 决定签名内容，两者要保持一致
        ossClient = OSSClient.newBuilder()
                .credentialsProvider(new EnvironmentVariableCredentialsProvider())
                .region(aliyunOssProperties.getRegion())
                .endpoint(aliyunOssProperties.getEndpoint())
                .build();
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (ossClient != null) {
            ossClient.close();
        }
    }

    /**
     * 上传文件到阿里云 OSS
     *
     * @param content          文件内容的字节数组
     * @param originalFilename 原始文件名，用于保留扩展名、推断 Content-Type
     * @return 上传后文件的访问 URL
     */
    public String upload(byte[] content, String originalFilename) {

        //原始文件名可能为 null 或带完整路径，先清洗成可用的文件名
        String filename = normalizeFilename(originalFilename);
        String objectName = buildObjectName(filename);

        PutObjectRequest request = PutObjectRequest.newBuilder()
                .bucket(aliyunOssProperties.getBucketName())
                .key(objectName)
                .body(BinaryData.fromBytes(content))
                .contentType(contentTypeOf(filename))
                .build();

        ossClient.putObject(request);

        //访问地址：https://{bucketName}.{endpoint}/{objectName}
        //文件名可能是中文、含空格，要编码过才能放进链接里
        String host = aliyunOssProperties.getEndpoint().replaceFirst("^https?://", "");
        String path = UriUtils.encodePath(objectName, StandardCharsets.UTF_8);
        return "https://" + aliyunOssProperties.getBucketName() + "." + host + "/" + path;
    }

    /**
     * 生成对象名：年月目录 + 原始文件名，重名时自动加序号
     * 如 2026/09/简历.pdf、2026/09/简历(1).pdf
     * OSS 没有真正的目录，对象名里的 "/" 会被控制台当作文件夹展示
     */
    private String buildObjectName(String filename) {

        //按年月分目录，如 2026/09/
        String folder = LocalDate.now().format(DATE_FOLDER);

        String objectName = folder + filename;
        if (!ossClient.doesObjectExist(aliyunOssProperties.getBucketName(), objectName)) {
            return objectName;
        }

        //序号加在扩展名之前：简历(1).pdf，而不是 简历.pdf(1)
        int dotIndex = filename.lastIndexOf('.');
        boolean hasSuffix = dotIndex > 0;
        String baseName = hasSuffix ? filename.substring(0, dotIndex) : filename;
        String suffix = hasSuffix ? filename.substring(dotIndex) : "";

        for (int i = 1; i <= MAX_RENAME_ATTEMPTS; i++) {
            String candidate = folder + baseName + "(" + i + ")" + suffix;
            if (!ossClient.doesObjectExist(aliyunOssProperties.getBucketName(), candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("同名文件过多，无法为 " + filename + " 生成唯一的对象名");
    }

    /**
     * 清洗原始文件名：去掉路径和控制字符，为空时退化成随机名
     */
    private String normalizeFilename(String originalFilename) {

        if (originalFilename == null || originalFilename.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }

        //少数浏览器会把完整路径一起传过来，只保留最后的文件名
        String filename = originalFilename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);

        //换行等控制字符会污染请求头，直接删掉
        filename = filename.replaceAll("[\\x00-\\x1f\\x7f]", "").trim();

        //文件名为 "."、".." 之类时无法作为对象名
        if (filename.isEmpty() || filename.equals(".") || filename.equals("..")) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return filename;
    }

    private String contentTypeOf(String originalFilename) {

        int dotIndex = originalFilename.lastIndexOf('.');
        String extension = dotIndex >= 0 ? originalFilename.substring(dotIndex + 1).toLowerCase() : "";
        return CONTENT_TYPE_MAP.getOrDefault(extension, "application/octet-stream");
    }
}
