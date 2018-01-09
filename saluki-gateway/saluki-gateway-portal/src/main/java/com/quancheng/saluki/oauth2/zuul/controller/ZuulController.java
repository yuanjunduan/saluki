/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.quancheng.saluki.oauth2.zuul.controller;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.quancheng.saluki.oauth2.common.BaseController;
import com.quancheng.saluki.oauth2.common.CommonResponse;
import com.quancheng.saluki.oauth2.common.Log;
import com.quancheng.saluki.oauth2.system.domain.PageDO;
import com.quancheng.saluki.oauth2.utils.FileType;
import com.quancheng.saluki.oauth2.zuul.dto.ZuulDto;
import com.quancheng.saluki.oauth2.zuul.service.ProtobufService;
import com.quancheng.saluki.oauth2.zuul.service.ZuulService;
import com.quancheng.saluki.oauth2.zuul.vo.ZuulVo;

/**
 * @author liushiming
 * @version ZuulController.java, v 0.0.1 2018年1月9日 上午11:19:14 liushiming
 */
@Controller
@RequestMapping("/gateway/zuul")
public class ZuulController extends BaseController {
  String prefix = "gateway/zuul";

  @Autowired
  private ProtobufService protobufService;

  @Autowired
  private ZuulService zuulService;

  @RequiresPermissions("gateway:zuul:zuul")
  @GetMapping()
  String role() {
    return prefix + "/zuul";
  }

  @Log("添加路由")
  @RequiresPermissions("gateway:zuul:add")
  @GetMapping("/add")
  String add() {
    return prefix + "/add";
  }

  @Log("编辑路由")
  @RequiresPermissions("gateway:zuul:edit")
  @GetMapping("/edit/{id}")
  String edit(@PathVariable("id") Long id, Model model) {
    // ZuulDto zuulDto = zuulService.get(id);
    // model.addAttribute("route", zuulDto);
    return prefix + "/edit";
  }


  @RequiresPermissions("gateway:zuul:zuul")
  @GetMapping("/list")
  @ResponseBody
  PageDO<ZuulVo> list(@RequestParam Map<String, Object> params) {
    // Query query = new Query(params);
    // PageDO<RouteDO> page = zuulService.queryList(query);
    return null;
  }

  @Log("保存路由")
  @RequiresPermissions("gateway:zuul:add")
  @PostMapping("/save")
  @ResponseBody()
  CommonResponse save(ZuulVo zuulVo,
      @RequestParam(name = "input", required = false) MultipartFile inputFile,
      @RequestParam(name = "output", required = false) MultipartFile outputFile,
      @RequestParam(name = "zipFile", required = false) MultipartFile zipFile) {
    try {
      if (zipFile != null) {
        InputStream directoryZipStream = zipFile.getInputStream();
        CommonResponse response = judgeFileType(directoryZipStream, "zip");
        if (response != null) {
          return response;
        } else {
          String serviceFileName = zuulVo.getServiceFileName();
          byte[] protoContext =
              protobufService.compileDirectoryProto(directoryZipStream, serviceFileName);
          ZuulDto zuulDto = zuulVo.buildZuulDto();
          zuulDto.setProtoContext(protoContext);
          zuulService.save(zuulDto);
        }
      } else if (inputFile != null && outputFile != null) {
        InputStream inputStream = inputFile.getInputStream();
        InputStream outputStream = outputFile.getInputStream();
        CommonResponse responseInput = judgeFileType(inputStream, "proto");
        CommonResponse responseOutput = judgeFileType(outputStream, "proto");
        if (responseInput != null) {
          return responseInput;
        } else if (responseOutput != null) {
          return responseOutput;
        } else {
          String fileNameInput = inputFile.getOriginalFilename();
          byte[] protoInput = protobufService.compileFileProto(inputStream, fileNameInput);
          String fileNameOutput = outputFile.getOriginalFilename();
          byte[] protoOutput = protobufService.compileFileProto(outputStream, fileNameOutput);
          ZuulDto zuulDto = zuulVo.buildZuulDto();
          zuulDto.setProtoReq(protoInput);
          zuulDto.setProtoRep(protoOutput);
          zuulService.save(zuulDto);
        }
      }
    } catch (IOException | IllegalAccessException | InvocationTargetException e) {
      return CommonResponse.error(1, e.getMessage());
    }
    return CommonResponse.ok();
  }

  private CommonResponse judgeFileType(InputStream inpustream, String type) throws IOException {
    String fileType = FileType.calculateFileHexString(inpustream);
    if (!type.equals(fileType)) {
      return CommonResponse.error(1, "只能上传" + type + "类型文件");
    } else {
      return null;
    }
  }


  // @Log("更新角色")
  // @RequiresPermissions("gateway:zuul:edit")
  // @PostMapping("/update")
  // @ResponseBody()
  // CommonResponse update(RoleDO role) {
  // if (roleService.update(role) > 0) {
  // return CommonResponse.ok();
  // } else {
  // return CommonResponse.error(1, "保存失败");
  // }
  // }
  //
  // @Log("删除角色")
  // @RequiresPermissions("gateway:zuul:remove")
  // @PostMapping("/remove")
  // @ResponseBody()
  // CommonResponse save(Long id) {
  // if (roleService.remove(id) > 0) {
  // return CommonResponse.ok();
  // } else {
  // return CommonResponse.error(1, "删除失败");
  // }
  // }
  //
  // @RequiresPermissions("gateway:zuul:batchRemove")
  // @Log("批量删除角色")
  // @PostMapping("/batchRemove")
  // @ResponseBody
  // CommonResponse batchRemove(@RequestParam("ids[]") Long[] ids) {
  // int r = roleService.batchremove(ids);
  // if (r > 0) {
  // return CommonResponse.ok();
  // }
  // return CommonResponse.error();
  // }
}