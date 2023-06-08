/**
 *    Copyright 2009-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

/**
 * 通用的 Token 解析器
 */
public class GenericTokenParser {

  /**
   * 开始的 Token 字符串
   */
  private final String openToken;
  /**
   * 结束的 Token 字符串
   */
  private final String closeToken;

  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  /**
   * 循环( 因为可能不只一个 )解析以 openToken 开始，以 closeToken 结束的 Token ，并提交给 handler 进行处理，即 <x> 处。
   * @param text
   * @return
   */
  public String parse(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    // 寻找开始的 openToken 的位置
    int start = text.indexOf(openToken, 0);
    if (start == -1) {
      // 找不到，直接返回
      return text;
    }
    char[] src = text.toCharArray();
    // 起始查找位置
    int offset = 0;
    // 结果
    final StringBuilder builder = new StringBuilder();
    StringBuilder expression = null;
    // 循环匹配
    while (start > -1) {
      // 转义字符
      if (start > 0 && src[start - 1] == '\\') {
        // 因为 openToken 前面一个位置是 \ 转义字符，所以忽略 \
        // 添加 [offset, start - offset - 1] 和 openToken 的内容，添加到 builder 中
        builder.append(src, offset, start - offset - 1).append(openToken);
        // 修改 offset
        offset = start + openToken.length();
      }
      // 非转义字符
      else {
        // 创建/重置 expression 对象
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        // 添加 offset 和 openToken 之间的内容，添加到 builder 中
        builder.append(src, offset, start - offset);
        // 修改 offset
        offset = start + openToken.length();
        // 寻找结束的 closeToken 的位置
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          // 转义
          if (end > offset && src[end - 1] == '\\') {
            // 因为 endToken 前面一个位置是 \ 转义字符，所以忽略 \
            // 添加 [offset, end - offset - 1] 和 endToken 的内容，添加到 builder 中
            expression.append(src, offset, end - offset - 1).append(closeToken);
            // 修改 offset
            offset = end + closeToken.length();
            // 继续，寻找结束的 closeToken 的位置
            end = text.indexOf(closeToken, offset);
          }
          // 非转义
          else {
            // 添加 [offset, end - offset] 的内容，添加到 builder 中
            expression.append(src, offset, end - offset);
            offset = end + closeToken.length();
            break;
          }
        }
        // 拼接内容
        if (end == -1) {
          // closeToken 未找到，直接拼接
          builder.append(src, start, src.length - start);
          // 修改 offset
          offset = src.length;
        } else {
          // <x> closeToken 找到，将 expression 提交给 handler 处理 ，并将处理结果添加到 builder 中
          builder.append(handler.handleToken(expression.toString()));
          // 修改 offset
          offset = end + closeToken.length();
        }
      }
      // 继续，寻找开始的 openToken 的位置
      start = text.indexOf(openToken, offset);
    }
    // 拼接剩余的部分
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
