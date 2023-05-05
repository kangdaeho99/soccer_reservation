package com.room.reservation.controller;

import com.room.reservation.dto.UploadResultDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@Log4j2
public class UploadController {
    @Value("${com.room.reservation.upload.path}")
    private String uploadPath;
    @PostMapping("/uploadAjax")
    public ResponseEntity<List<UploadResultDTO>> uploadFile(MultipartFile[] uploadFiles){

        List<UploadResultDTO> resultDTOList = new ArrayList<>();
        for(MultipartFile uploadFile : uploadFiles){
            //이미지 파일만 업로드 가능
            if(uploadFile.getContentType().startsWith("image") == false){
                log.warn("this file is not image type");
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }

            //실제 파일 이름은 IE나 Edge는 전체 경로가 들어오므로
            String originalName = uploadFile.getOriginalFilename();
            String fileName = originalName.substring(originalName.lastIndexOf("\\") + 1);

            log.info("fileName: "+fileName);
            //날짜 폴더 생성
            String folderPath = makeFolder();
            //UUID
            String uuid = UUID.randomUUID().toString();

            //저장할 파일 이름 중간에 "_"를 이용해서 구분
            String saveName = uploadPath + File.separator + folderPath + File.separator + uuid + "_" + fileName;
            Path savePath = Paths.get(saveName);

            try{
                uploadFile.transferTo(savePath); //실제 이미지 저장
                resultDTOList.add(new UploadResultDTO(fileName, uuid, folderPath));
            } catch(IOException e){
                e.printStackTrace();
            }
        } // end for
        return new ResponseEntity<>(resultDTOList, HttpStatus.OK);
    }

    /**
     * Description : URL인코딩된 파일이름을 파라미터로 받아서 해당 파일을 byte[]로 만들어서 브라우저에 전송합니다.
     * JSON으로 반환된 업로드 결과를 화면에서 확인하기 위해 만든 함수입니다.
     * 1) 브라우저에서 링크를 통해 <img> 태그를 추가합니다.
     * 2) 서버에서 해당 URL이 호출되는 경우에 이미지 파일 데이터를 브라우저로 전송해줍니다.
     *
     */
    @GetMapping("/display")
    public ResponseEntity<byte[]> getFile(String fileName){
        ResponseEntity<byte[]> result = null;

        try{
            String srcFileName = URLDecoder.decode(fileName, "UTF-8");
            log.info("fileName : " + srcFileName);
            File file = new File(uploadPath + File.separator + srcFileName);
            log.info("file : "+ file);
            HttpHeaders header = new HttpHeaders();

            //MIME 타입 처리
            //파일 확장자에 따라서 브라우저에 전송하는 MIME타입이 달라져야하는
            // 문제는 java.nio.file 패키지의 Files.probeContentType()을 이용해서 처리합니다.
            header.add("Content-Type", Files.probeContentType(file.toPath()));
            //파일 데이터 처리
            //스프링에서 제공하는 org.springframework.util.FileCopyUtils를 이용해서 처리합니다.
            //브라우저에서 업로드된 결과중에 imageUrl 속성 존재.(UploadResultDTO의 getImageUrl())
            result = new ResponseEntity<>(FileCopyUtils.copyToByteArray(file), header, HttpStatus.OK);
        } catch(Exception e){
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return result;
    }

    private String makeFolder(){
        String str = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String folderPath = str.replace("/", File.separator);

        //make Folder ---------
        File uploadPathFolder = new File(uploadPath, folderPath);

        if(uploadPathFolder.exists() == false){
            uploadPathFolder.mkdirs();
        }
        return folderPath;
    }
}
