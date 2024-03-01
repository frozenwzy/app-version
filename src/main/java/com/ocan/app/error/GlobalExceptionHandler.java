package com.ocan.app.error;

import com.ocan.app.mode.Result;
import org.apache.commons.io.FileExistsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //文件上传异常
    @ExceptionHandler(FileExistsException.class)
    public Result<String> handlerFileExistsException(FileExistsException e) {
        return Result.error("上传失败，上传的文件中存在相同的文件！");
    }


    //通用异常处理
    @ExceptionHandler(Exception.class)
    public Result<String> handlerException(Exception e){

        return Result.error(e.toString());
    }
}
