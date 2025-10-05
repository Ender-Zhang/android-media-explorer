#!/usr/bin/env python3
"""
MediaExplorer ADB 传输客户端
通过ADB端口转发高速传输文件

使用方法：
1. 在手机应用中选择文件并启动ADB传输
2. 运行此脚本：python3 adb_transfer_client.py [下载目录]
"""

import socket
import json
import os
import sys
import subprocess
from pathlib import Path
from datetime import datetime

class AdbTransferClient:
    def __init__(self, host='localhost', port=12345):
        self.host = host
        self.port = port
    
    def setup_port_forward(self):
        """设置ADB端口转发"""
        try:
            print("🔧 设置ADB端口转发...")
            result = subprocess.run(
                ['adb', 'forward', f'tcp:{self.port}', f'tcp:{self.port}'],
                capture_output=True,
                text=True
            )
            if result.returncode == 0:
                print(f"✅ 端口转发成功: {self.port} -> {self.port}")
                return True
            else:
                print(f"❌ 端口转发失败: {result.stderr}")
                return False
        except FileNotFoundError:
            print("❌ 找不到ADB命令，请确保已安装Android SDK Platform Tools")
            print("   下载地址: https://developer.android.com/studio/releases/platform-tools")
            return False
        except Exception as e:
            print(f"❌ 端口转发错误: {e}")
            return False
    
    def connect(self):
        """连接到手机服务器"""
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.connect((self.host, self.port))
            return sock
        except Exception as e:
            print(f"❌ 连接失败: {e}")
            print("   请确保:")
            print("   1. 手机已通过USB连接")
            print("   2. 已启用USB调试")
            print("   3. 应用中已启动ADB传输")
            return None
    
    def send_command(self, sock, command):
        """发送命令"""
        sock.sendall(f"{command}\n".encode())
    
    def get_file_list(self):
        """获取文件列表"""
        sock = self.connect()
        if not sock:
            return None
        
        try:
            self.send_command(sock, "LIST")
            
            # 使用文件对象按行读取
            sock_file = sock.makefile('rb')
            
            # 读取响应状态
            status = sock_file.readline().decode().strip()
            if status != "OK":
                print(f"❌ 获取文件列表失败: {status}")
                sock.close()
                return None
            
            # 读取数据大小
            size_line = sock_file.readline().decode().strip()
            data_size = int(size_line)
            
            # 读取JSON数据
            data = sock_file.read(data_size)
            
            file_list = json.loads(data.decode())
            sock_file.close()
            sock.close()
            return file_list
            
        except Exception as e:
            print(f"❌ 获取文件列表错误: {e}")
            import traceback
            traceback.print_exc()
            sock.close()
            return None
    
    def download_file(self, index, save_dir):
        """下载单个文件"""
        sock = self.connect()
        if not sock:
            return False
        
        try:
            self.send_command(sock, f"GET {index}")
            
            # 使用文件对象按行读取
            sock_file = sock.makefile('rb')
            
            # 读取响应状态
            status = sock_file.readline().decode().strip()
            if status != "OK":
                error_msg = sock_file.readline().decode().strip()
                print(f"❌ 下载失败: {error_msg}")
                sock.close()
                return False
            
            # 读取文件大小
            size_line = sock_file.readline().decode().strip()
            file_size = int(size_line)
            
            # 读取文件名
            filename_line = sock_file.readline().decode().strip()
            filename = filename_line
            
            # 确保保存目录存在
            os.makedirs(save_dir, exist_ok=True)
            
            # 保存文件
            filepath = os.path.join(save_dir, filename)
            
            # 如果文件存在，添加序号
            if os.path.exists(filepath):
                base, ext = os.path.splitext(filename)
                counter = 1
                while os.path.exists(filepath):
                    filepath = os.path.join(save_dir, f"{base}_{counter}{ext}")
                    counter += 1
            
            print(f"📥 下载: {filename} ({self.format_size(file_size)})")
            
            # 接收文件内容
            received = 0
            with open(filepath, 'wb') as f:
                while received < file_size:
                    chunk_size = min(65536, file_size - received)
                    chunk = sock_file.read(chunk_size)
                    if not chunk:
                        break
                    f.write(chunk)
                    received += len(chunk)
                    
                    # 显示进度
                    progress = (received / file_size) * 100
                    bar_length = 40
                    filled = int(bar_length * received / file_size)
                    bar = '█' * filled + '░' * (bar_length - filled)
                    print(f"\r   [{bar}] {progress:.1f}%", end='', flush=True)
            
            print()  # 换行
            
            if received == file_size:
                print(f"✅ 下载完成: {filepath}")
                sock_file.close()
                sock.close()
                return True
            else:
                print(f"⚠️  下载不完整: {received}/{file_size} 字节")
                sock_file.close()
                sock.close()
                return False
            
        except Exception as e:
            print(f"❌ 下载错误: {e}")
            import traceback
            traceback.print_exc()
            try:
                sock.close()
            except:
                pass
            return False
    
    def download_all(self, save_dir):
        """下载所有文件"""
        print("\n📱 获取文件列表...")
        file_list = self.get_file_list()
        
        if not file_list:
            return False
        
        print(f"\n📋 找到 {len(file_list)} 个文件:")
        for i, file_info in enumerate(file_list):
            file_type = "🎬" if file_info['type'] == 'video' else "🖼️"
            size_str = self.format_size(file_info['size'])
            print(f"   {i+1}. {file_type} {file_info['name']} ({size_str})")
        
        print(f"\n💾 保存目录: {os.path.abspath(save_dir)}")
        print("\n开始下载...\n")
        
        success_count = 0
        fail_count = 0
        start_time = datetime.now()
        
        for i, file_info in enumerate(file_list):
            print(f"[{i+1}/{len(file_list)}] ", end='')
            if self.download_file(i, save_dir):
                success_count += 1
            else:
                fail_count += 1
            print()
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        print("\n" + "="*60)
        print("📊 传输完成!")
        print(f"   ✅ 成功: {success_count} 个文件")
        if fail_count > 0:
            print(f"   ❌ 失败: {fail_count} 个文件")
        print(f"   ⏱️  用时: {duration:.1f} 秒")
        print("="*60)
        
        return fail_count == 0
    
    @staticmethod
    def format_size(size):
        """格式化文件大小"""
        for unit in ['B', 'KB', 'MB', 'GB']:
            if size < 1024.0:
                return f"{size:.2f} {unit}"
            size /= 1024.0
        return f"{size:.2f} TB"

def main():
    print("="*60)
    print("📱 MediaExplorer ADB 文件传输客户端")
    print("="*60)
    
    # 获取保存目录
    if len(sys.argv) > 1:
        save_dir = sys.argv[1]
    else:
        save_dir = os.path.join(os.path.expanduser("~"), "Downloads", "MediaExplorer")
    
    client = AdbTransferClient()
    
    # 设置端口转发
    if not client.setup_port_forward():
        print("\n💡 提示: 如果端口转发失败，请尝试:")
        print("   1. 确保只连接了一台Android设备")
        print("   2. 手动运行: adb forward tcp:12345 tcp:12345")
        print("   3. 检查USB调试是否已启用")
        return
    
    print()
    
    # 下载所有文件
    client.download_all(save_dir)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️  传输已取消")
    except Exception as e:
        print(f"\n❌ 程序错误: {e}")
        import traceback
        traceback.print_exc()

